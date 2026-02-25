package app.desperse.core.wallet

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.solana.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterClient
import com.solana.mobilewalletadapter.clientlib.scenario.LocalAssociationIntentCreator
import com.solana.mobilewalletadapter.clientlib.scenario.LocalAssociationScenario
import com.solana.publickey.SolanaPublicKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Manages all MWA (Mobile Wallet Adapter) protocol interactions.
 *
 * Handles authorization, message signing, and transaction signing/sending
 * via external Solana wallet apps (Phantom, Solflare, Seed Vault, etc.)
 * using the LocalAssociationScenario approach.
 *
 * Auth tokens are persisted in EncryptedSharedPreferences keyed per wallet address
 * for reauthorization on subsequent sessions.
 */
@Singleton
class MwaManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    companion object {
        private const val TAG = "MwaManager"
        private const val IDENTITY_URI = "https://desperse.com"
        private const val ICON_URI = "/icon.png"
        private const val IDENTITY_NAME = "Desperse"
        private const val CHAIN = "solana:mainnet"
        private const val MWA_SCHEME = "solana-wallet"
        private const val SEED_VAULT_PACKAGE = "com.solanamobile.seedvaultimpl"
        private const val ASSOCIATION_TIMEOUT_MS = 30_000L
        private const val CLIENT_TIMEOUT_MS = 45_000L
        private const val AUTH_TOKEN_PREFS_NAME = "mwa_auth_tokens"
        private const val AUTH_TOKEN_KEY_PREFIX = "auth_token_"
        private const val MAX_REAUTH_ATTEMPTS = 3

        private const val SEEKER_WALLET_PACKAGE = "com.solanamobile.wallet"

        /** Extended timeout for Seeker Wallet (TEE biometric confirmation is slower) */
        private const val SEEKER_ASSOCIATION_TIMEOUT_MS = 60_000L

        /** Known MWA wallet URI hosts to friendly display names */
        private val WALLET_NAME_MAP = mapOf(
            "phantom.app" to "Phantom",
            "solflare.com" to "Solflare",
            "solflare.dev" to "Solflare",
            "ultimate.app" to "Ultimate",
            "glow.app" to "Glow",
            "tiplink.io" to "TipLink",
            "jup.ag" to "Jupiter",
        )

        /** Known MWA wallet package names to friendly display names (fallback when walletUriBase is null) */
        private val WALLET_PACKAGE_MAP = mapOf(
            "app.phantom" to "Phantom",
            "com.solflare.mobile" to "Solflare",
            SEEKER_WALLET_PACKAGE to "Seeker Wallet",
            "com.ultimate.app" to "Ultimate",
            "com.glow.app" to "Glow",
            "ag.jup.mobile" to "Jupiter",
            "ag.jup.jupiter.android" to "Jupiter",
        )

        /** Packages that should be excluded from MWA intent resolution.
         *  Seed Vault: TEE system service, not a user-facing wallet.
         *  Backpack: MWA broken (ECONNREFUSED) and deeplinks non-functional on both variants. */
        private val EXCLUDED_PACKAGES = setOf(
            SEED_VAULT_PACKAGE,                  // com.solanamobile.seedvaultimpl — TEE service
            "app.backpack.mobile",               // Play Store Backpack — broken MWA
            "app.backpack.mobile.standalone",    // Seeker pre-install — broken MWA + deeplinks
        )

        /** Known MWA wallet URI hosts to lowercase Privy walletClientType identifiers */
        private val WALLET_CLIENT_TYPE_MAP = mapOf(
            "phantom.app" to "phantom",
            "solflare.com" to "solflare",
            "solflare.dev" to "solflare",
            "ultimate.app" to "ultimate",
            "glow.app" to "glow",
            "tiplink.io" to "tiplink",
            "jup.ag" to "jupiter",
        )

        /** Known MWA wallet package names to lowercase Privy walletClientType identifiers */
        private val WALLET_PACKAGE_CLIENT_TYPE_MAP = mapOf(
            "app.phantom" to "phantom",
            "com.solflare.mobile" to "solflare",
            SEEKER_WALLET_PACKAGE to "seeker",
            "com.ultimate.app" to "ultimate",
            "com.glow.app" to "glow",
            "ag.jup.mobile" to "jupiter",
            "ag.jup.jupiter.android" to "jupiter",
        )
    }

    // Cached wallet availability to avoid repeated PackageManager queries
    @Volatile
    private var cachedAvailability: Boolean? = null
    @Volatile
    private var cachedWallets: List<InstalledMwaWallet>? = null
    @Volatile
    private var lastWalletQueryTime = 0L
    private val WALLET_CACHE_TTL_MS = 5_000L

    // Rate limiting: track reauthorization attempts per session
    @Volatile
    private var reauthAttemptCount = 0

    // Package name of the wallet app that handled the last MWA intent (fallback for wallet name)
    @Volatile
    private var lastResolvedWalletPackage: String? = null

    // Stored walletUriBase from the last authorize response (used as uriPrefix to target a specific wallet)
    @Volatile
    private var lastWalletUriBase: Uri? = null

    // ActivityResultLauncher for launching wallet intents (set from MainActivity).
    // Uses startActivityForResult like the official MWA SDK, providing better lifecycle handling.
    @Volatile
    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null

    /**
     * Set the ActivityResultLauncher for launching MWA wallet intents.
     * Must be called from Activity.onCreate() after registering with registerForActivityResult().
     * This aligns with the official MWA SDK approach (startActivityForResult, not startActivity).
     */
    fun setActivityResultLauncher(launcher: ActivityResultLauncher<Intent>?) {
        activityResultLauncher = launcher
        Log.d(TAG, "ActivityResultLauncher ${if (launcher != null) "set" else "cleared"}")
    }

    // Lazy-initialized EncryptedSharedPreferences for MWA auth tokens
    private val authTokenPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                AUTH_TOKEN_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create encrypted prefs for MWA auth tokens, using fallback", e)
            appContext.getSharedPreferences("${AUTH_TOKEN_PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    // ========================================================================
    // Public API
    // ========================================================================

    /**
     * Check if any MWA-compatible wallet app is installed on the device.
     * Resolves the "solana-wallet-adapter" intent action via PackageManager.
     */
    fun isAvailable(): Boolean {
        val now = System.currentTimeMillis()
        val cached = cachedAvailability
        if (cached != null && (now - lastWalletQueryTime) < WALLET_CACHE_TTL_MS) {
            return cached
        }
        return try {
            val wallets = queryMwaWallets()
            Log.d(TAG, "isAvailable: found ${wallets.size} MWA wallet(s): ${wallets.map { it.activityInfo?.packageName }}")
            val result = wallets.isNotEmpty()
            cachedAvailability = result
            lastWalletQueryTime = now
            result
        } catch (e: Exception) {
            Log.w(TAG, "Error checking MWA availability", e)
            cachedAvailability ?: false
        }
    }

    /**
     * Returns a list of installed MWA-compatible wallet apps (excluding system services like Seed Vault).
     * Each entry contains the package name, display name, and Privy wallet client type identifier.
     */
    fun getInstalledWallets(): List<InstalledMwaWallet> {
        val now = System.currentTimeMillis()
        val cached = cachedWallets
        if (cached != null && (now - lastWalletQueryTime) < WALLET_CACHE_TTL_MS) {
            return cached
        }
        return try {
            val resolveInfos = queryMwaWallets()
            val result = resolveInfos.mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val displayName = WALLET_PACKAGE_MAP[pkg]
                    ?: resolveInfo.loadLabel(appContext.packageManager)?.toString()
                    ?: pkg
                val clientType = WALLET_PACKAGE_CLIENT_TYPE_MAP[pkg] ?: "unknown"
                InstalledMwaWallet(
                    packageName = pkg,
                    displayName = displayName,
                    walletClientType = clientType
                )
            }
            cachedWallets = result
            cachedAvailability = result.isNotEmpty()
            lastWalletQueryTime = now
            result
        } catch (e: Exception) {
            Log.w(TAG, "Error getting installed wallets", e)
            cachedWallets ?: emptyList()
        }
    }

    /**
     * Query MWA-compatible wallet apps, filtering out excluded packages (e.g. Seed Vault).
     */
    private fun queryMwaWallets(): List<android.content.pm.ResolveInfo> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$MWA_SCHEME://"))
        val resolveInfos = appContext.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfos.filter { info ->
            val pkg = info.activityInfo?.packageName
            pkg != null && pkg !in EXCLUDED_PACKAGES
        }
    }

    /**
     * Check if the device is a Saga / Seeker with Seed Vault installed.
     * Uses Build properties first (most reliable), then Seed Vault package as fallback.
     */
    fun isSeekerDevice(): Boolean {
        // Primary: hardware identifiers (Seeker ships with solanamobile brand)
        if (android.os.Build.MODEL == "Seeker" || android.os.Build.BRAND == "solanamobile") {
            Log.d(TAG, "isSeekerDevice: Detected via Build properties (model=${android.os.Build.MODEL}, brand=${android.os.Build.BRAND})")
            return true
        }
        // Fallback: Seed Vault package (Saga devices)
        return try {
            appContext.packageManager.getPackageInfo(SEED_VAULT_PACKAGE, 0)
            Log.d(TAG, "isSeekerDevice: Seed Vault package FOUND")
            true
        } catch (_: PackageManager.NameNotFoundException) {
            Log.d(TAG, "isSeekerDevice: Not a Solana Mobile device")
            false
        }
    }

    /**
     * Whether a specific wallet package is the Seeker Wallet (needs extended timeout).
     */
    private fun isSeekerWallet(packageName: String?): Boolean {
        return packageName == SEEKER_WALLET_PACKAGE
    }

    /**
     * Authorize with an external wallet via MWA.
     *
     * Establishes a local association, sends the authorize request with Desperse identity,
     * and returns the wallet's public key (base58), auth token, and label.
     *
     * @param activity The current activity, used to launch the wallet app intent.
     * @param targetPackage Optional: target a specific wallet app by package name.
     *   When provided, the MWA intent is sent directly to this package (no system chooser).
     * @return [Result] containing [MwaAuthResult] on success, or an [MwaError] on failure.
     */
    suspend fun authorize(activity: Activity, targetPackage: String? = null): Result<MwaAuthResult> {
        return safeMwaTransact(activity, targetPackage) { client ->
            // 8-param authorize: identityUri, iconUri, identityName, chain, authToken, features, addresses, signInPayload
            val authResult = client.authorize(
                Uri.parse(IDENTITY_URI),
                Uri.parse(ICON_URI),
                IDENTITY_NAME,
                CHAIN,  // chain — "solana:mainnet"
                null,   // authToken — null for fresh authorization
                null,   // features
                null,   // addresses
                null    // signInPayload
            ).get()

            val publicKeyBytes = authResult.publicKey
            val solanaPublicKey = SolanaPublicKey(publicKeyBytes)
            val address = solanaPublicKey.base58()
            val authToken = authResult.authToken

            // Persist walletUriBase for future targeted connections
            if (authResult.walletUriBase != null) {
                lastWalletUriBase = authResult.walletUriBase
                saveWalletUriBase(address, authResult.walletUriBase.toString())
            }

            val walletLabel = resolveWalletName(authResult.walletUriBase)

            // Persist auth token for future reauthorization
            if (authToken != null) {
                saveAuthToken(address, authToken)
            }

            MwaAuthResult(
                address = address,
                authToken = authToken,
                walletLabel = walletLabel,
                walletClientType = resolveWalletClientType(authResult.walletUriBase),
                publicKeyBytes = publicKeyBytes
            )
        }
    }

    /**
     * Authorize and sign a message in a single MWA session (one wallet redirect).
     *
     * Optimized for SIWS login: authorizes to get the wallet address, calls
     * [messageProvider] to fetch a challenge, then signs it — all within a single
     * LocalAssociationScenario so the user only leaves the app once.
     *
     * If [messageProvider] throws (e.g., Privy foreground requirement), the session
     * still captures the authorization result and returns [MwaError.MessageProviderFailed]
     * so the caller can fall back to a two-step flow without re-authorizing.
     *
     * @param activity The current activity for launching the wallet intent.
     * @param messageProvider Called with the wallet address after authorization.
     *   Should return the message bytes to sign. May throw to abort the flow.
     * @return [Result] containing [MwaAuthResult] and signature bytes on success,
     *   or [MwaError.MessageProviderFailed] if only authorization succeeded.
     */
    suspend fun authorizeAndSignMessage(
        activity: Activity,
        targetPackage: String? = null,
        messageProvider: suspend (address: String) -> ByteArray
    ): Result<Pair<MwaAuthResult, ByteArray>> {
        return safeMwaTransact(activity, targetPackage) { client ->
            // Step 1: Authorize with wallet
            // 8-param: identityUri, iconUri, identityName, chain, authToken, features, addresses, signInPayload
            val authResult = client.authorize(
                Uri.parse(IDENTITY_URI),
                Uri.parse(ICON_URI),
                IDENTITY_NAME,
                CHAIN,  // chain — "solana:mainnet"
                null,   // authToken — null for fresh authorization
                null,   // features
                null,   // addresses
                null    // signInPayload
            ).get()

            val publicKeyBytes = authResult.publicKey
            val solanaPublicKey = SolanaPublicKey(publicKeyBytes)
            val address = solanaPublicKey.base58()
            val authToken = authResult.authToken

            if (authToken != null) {
                saveAuthToken(address, authToken)
            }
            // Persist walletUriBase for future targeted connections
            if (authResult.walletUriBase != null) {
                lastWalletUriBase = authResult.walletUriBase
                saveWalletUriBase(address, authResult.walletUriBase.toString())
            }
            reauthAttemptCount = 0

            val mwaAuth = MwaAuthResult(
                address = address,
                authToken = authToken,
                walletLabel = resolveWalletName(authResult.walletUriBase),
                walletClientType = resolveWalletClientType(authResult.walletUriBase),
                publicKeyBytes = publicKeyBytes
            )

            // Step 2: Get the message to sign (caller fetches SIWS challenge)
            // If this fails (e.g., Privy needs foreground), capture the auth result
            // so the caller can fall back to a two-step flow.
            val message = try {
                messageProvider(address)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.w(TAG, "messageProvider failed (auth succeeded), enabling two-step fallback", e)
                throw MwaError.MessageProviderFailed(mwaAuth, e)
            }

            // Step 3: Sign message in the same session (already authorized)
            val signResult = client.signMessagesDetached(
                arrayOf(message),
                arrayOf(publicKeyBytes)
            ).get()

            Pair(mwaAuth, signResult.messages.first().signatures.first())
        }
    }

    /**
     * Sign an arbitrary message (e.g., SIWS authentication challenge).
     *
     * Attempts reauthorization with a stored auth token first; falls back to full
     * authorization if reauthorization fails or no token is available.
     *
     * @param activity The current activity for launching the wallet intent.
     * @param message The raw message bytes to sign.
     * @param targetPackage Optional: target a specific wallet app by package name.
     * @return [Result] containing the detached signature bytes on success.
     */
    suspend fun signMessage(activity: Activity, message: ByteArray, targetPackage: String? = null): Result<ByteArray> {
        return safeMwaTransact(activity, targetPackage) { client ->
            val authResult = reauthorizeOrAuthorize(client)
            val addresses = arrayOf(authResult.publicKeyBytes)

            val signResult = client.signMessagesDetached(
                arrayOf(message),
                addresses
            ).get()

            signResult.messages.first().signatures.first()
        }
    }

    /**
     * Sign a serialized Solana transaction via the external wallet (sign only, no broadcast).
     *
     * Returns the signed transaction bytes. The caller is responsible for broadcasting
     * via SolanaRpcClient. This avoids the deprecated signAndSendTransactions API
     * which some wallets (e.g., Phantom) don't handle reliably.
     *
     * @param activity The current activity for launching the wallet intent.
     * @param txBytes The serialized unsigned transaction bytes.
     * @param targetPackage Optional: target a specific wallet app by package name.
     * @return [Result] containing the signed transaction bytes on success.
     */
    suspend fun signTransaction(activity: Activity, txBytes: ByteArray, targetPackage: String? = null): Result<ByteArray> {
        return safeMwaTransact(activity, targetPackage) { client ->
            val authResult = reauthorizeOrAuthorize(client)

            val signResult = client.signTransactions(arrayOf(txBytes)).get()
            signResult.signedPayloads.first()
        }
    }

    /**
     * Reset the per-session reauthorization attempt counter.
     * Call this when the user starts a new logical session (e.g., after fresh authorize).
     */
    fun resetReauthCounter() {
        reauthAttemptCount = 0
    }

    /**
     * Clear all stored MWA auth tokens (e.g., on logout).
     */
    suspend fun clearAuthTokens() {
        withContext(Dispatchers.IO) {
            authTokenPrefs.edit().clear().apply()
        }
        reauthAttemptCount = 0
        Log.d(TAG, "Cleared all MWA auth tokens")
    }

    // ========================================================================
    // Private helpers
    // ========================================================================

    /**
     * Resolve the exact MWA handler activity for a specific wallet package.
     *
     * Queries PackageManager for activities handling the solana-wallet:// intent
     * and returns the ComponentName for the matching activity in the target package.
     * This is more precise than setPackage() because it targets the exact activity
     * that registered for MWA, avoiding accidental routing to the wrong activity.
     *
     * The official MWA SDK never uses setPackage() — it relies on URI-based routing.
     * setComponent() is the closest equivalent when we need to target a specific wallet.
     */
    private fun resolveMwaHandlerComponent(targetPackage: String): ComponentName? {
        return try {
            val queryIntent = Intent(Intent.ACTION_VIEW, Uri.parse("$MWA_SCHEME://"))
            val resolveInfos = appContext.packageManager.queryIntentActivities(
                queryIntent, PackageManager.MATCH_DEFAULT_ONLY
            )

            // Log all resolved MWA handlers for diagnostics
            val allHandlers = resolveInfos.mapNotNull { ri ->
                ri.activityInfo?.let { "${it.packageName}/${it.name}" }
            }
            Log.d(TAG, "All MWA handlers for solana-wallet://: $allHandlers")

            val matchingInfo = resolveInfos.find { it.activityInfo?.packageName == targetPackage }
            if (matchingInfo?.activityInfo != null) {
                val ai = matchingInfo.activityInfo
                ComponentName(ai.packageName, ai.name).also {
                    Log.d(TAG, "Resolved MWA handler for $targetPackage: ${ai.name}")
                }
            } else {
                Log.w(TAG, "No MWA handler activity found for package $targetPackage " +
                    "(available: ${allHandlers.map { it.substringBefore('/') }})")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve MWA handler for $targetPackage", e)
            null
        }
    }

    /**
     * Core transact wrapper that establishes a LocalAssociationScenario,
     * launches the wallet app, executes the [block] with the MWA client,
     * and maps all exceptions to typed [MwaError] instances.
     *
     * IMPORTANT: Never catches [CancellationException] to preserve structured concurrency.
     */
    private suspend fun <T> safeMwaTransact(
        activity: Activity,
        targetPackage: String? = null,
        block: suspend (MobileWalletAdapterClient) -> T
    ): Result<T> {
        return withContext(Dispatchers.IO) {
            var scenario: LocalAssociationScenario? = null
            try {
                // Use extended timeout for Seeker Wallet (TEE biometric is slower)
                val timeoutMs = if (isSeekerWallet(targetPackage)) {
                    SEEKER_ASSOCIATION_TIMEOUT_MS
                } else {
                    ASSOCIATION_TIMEOUT_MS
                }
                scenario = LocalAssociationScenario(timeoutMs.toInt())

                // Use stored walletUriBase as uriPrefix when targeting a previously-connected wallet,
                // or null to use the default solana-wallet:// scheme
                val uriPrefix = if (targetPackage != null) {
                    getStoredWalletUriBase(targetPackage)?.let { Uri.parse(it) }
                } else {
                    lastWalletUriBase
                }

                val associationIntent = LocalAssociationIntentCreator.createAssociationIntent(
                    uriPrefix,
                    scenario.port,
                    scenario.session
                )

                // Target a specific wallet via resolved MWA handler component.
                // The official MWA SDK never uses setPackage() — it relies on URI-based
                // routing. setComponent() is more precise: it targets the exact activity
                // that registered for solana-wallet://, avoiding misrouting to wrong
                // activities within a package (which caused ECONNREFUSED on some wallets).
                if (targetPackage != null) {
                    lastResolvedWalletPackage = targetPackage
                    val mwaComponent = resolveMwaHandlerComponent(targetPackage)
                    if (mwaComponent != null) {
                        associationIntent.component = mwaComponent
                        Log.d(TAG, "Targeting MWA handler: $mwaComponent, uriPrefix: $uriPrefix")
                    } else {
                        // No explicit MWA handler found for this package.
                        // Don't force setPackage — let Android resolve the full association URI.
                        // The wallet might still handle it via a different intent filter path.
                        Log.w(TAG, "No MWA handler for $targetPackage via solana-wallet:// query. " +
                            "Launching without package/component constraint (Android chooser may appear).")
                    }
                } else {
                    // No target specified — resolve which wallet will handle this intent
                    // (for wallet name fallback). Filter out excluded packages (e.g. Seed Vault).
                    lastResolvedWalletPackage = try {
                        val resolveInfos = appContext.packageManager.queryIntentActivities(
                            associationIntent, PackageManager.MATCH_DEFAULT_ONLY
                        )
                        val validPackages = resolveInfos
                            .mapNotNull { it.activityInfo?.packageName }
                            .filter { it !in EXCLUDED_PACKAGES }
                        val knownPackages = validPackages.filter { it in WALLET_PACKAGE_MAP }
                        Log.d(TAG, "MWA intent resolves to ${resolveInfos.size} app(s), " +
                            "${knownPackages.size} known: $knownPackages " +
                            "(excluded: ${resolveInfos.size - validPackages.size})")
                        if (knownPackages.size == 1) knownPackages.first() else null
                    } catch (_: Exception) { null }
                    Log.d(TAG, "Resolved wallet package: $lastResolvedWalletPackage")
                }

                Log.d(TAG, "Launching wallet intent: action=${associationIntent.action}, " +
                    "data=${associationIntent.data}, component=${associationIntent.component}, " +
                    "pkg=${associationIntent.`package`}")

                // Launch the wallet intent. Prefer ActivityResultLauncher (startActivityForResult)
                // which aligns with the official MWA SDK approach and provides better window/lifecycle
                // management. Fall back to startActivity if no launcher was registered.
                withContext(Dispatchers.Main) {
                    try {
                        val launcher = activityResultLauncher
                        if (launcher != null) {
                            launcher.launch(associationIntent)
                            Log.d(TAG, "ActivityResultLauncher.launch() succeeded")
                        } else {
                            activity.startActivity(associationIntent)
                            Log.d(TAG, "startActivity() succeeded (no launcher registered)")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to launch wallet intent", e)
                        throw e
                    }
                }

                val client = scenario.start()
                    .get(CLIENT_TIMEOUT_MS, TimeUnit.MILLISECONDS)

                val result = block(client)
                Result.success(result)
            } catch (e: CancellationException) {
                // NEVER swallow CancellationException — rethrow for structured concurrency
                throw e
            } catch (e: MwaError.MessageProviderFailed) {
                // Pass through directly — caller needs the auth result for fallback
                Log.d(TAG, "MWA authorize succeeded but messageProvider failed, returning for two-step fallback")
                Result.failure(e)
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "No MWA wallet app found", e)
                Result.failure(MwaError.NoWalletInstalled)
            } catch (e: TimeoutException) {
                Log.e(TAG, "MWA association timed out", e)
                Result.failure(MwaError.Timeout)
            } catch (e: java.util.concurrent.TimeoutException) {
                Log.e(TAG, "MWA client operation timed out", e)
                Result.failure(MwaError.Timeout)
            } catch (e: Exception) {
                val mwaError = mapExceptionToMwaError(e)
                Log.e(TAG, "MWA transact failed: ${mwaError::class.simpleName}", e)
                Result.failure(mwaError)
            } finally {
                try {
                    scenario?.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing MWA scenario", e)
                }
            }
        }
    }

    /**
     * Attempt reauthorization with a stored auth token. If the stored token is unavailable
     * or reauthorization fails (or rate limit exceeded), fall back to full authorization.
     */
    private suspend fun reauthorizeOrAuthorize(
        client: MobileWalletAdapterClient
    ): MwaAuthResult {
        // Try reauthorization if we have a stored token and haven't exceeded rate limit
        val storedToken = getLastUsedAuthToken()
        if (storedToken != null && reauthAttemptCount < MAX_REAUTH_ATTEMPTS) {
            try {
                reauthAttemptCount++
                val reauthResult = client.reauthorize(
                    Uri.parse(IDENTITY_URI),
                    Uri.parse(ICON_URI),
                    IDENTITY_NAME,
                    storedToken
                ).get()

                val publicKeyBytes = reauthResult.publicKey
                val solanaPublicKey = SolanaPublicKey(publicKeyBytes)
                val address = solanaPublicKey.base58()
                val newAuthToken = reauthResult.authToken

                // Update stored token if wallet issued a new one
                if (newAuthToken != null) {
                    saveAuthToken(address, newAuthToken)
                }

                Log.d(TAG, "Reauthorization successful for ${address.take(8)}...")
                return MwaAuthResult(
                    address = address,
                    authToken = newAuthToken,
                    walletLabel = resolveWalletName(reauthResult.walletUriBase),
                    walletClientType = resolveWalletClientType(reauthResult.walletUriBase),
                    publicKeyBytes = publicKeyBytes
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Reauthorization failed, falling back to full authorize", e)
            }
        }

        // Full authorization fallback
        // 8-param: identityUri, iconUri, identityName, chain, authToken, features, addresses, signInPayload
        val fullAuthResult = client.authorize(
            Uri.parse(IDENTITY_URI),
            Uri.parse(ICON_URI),
            IDENTITY_NAME,
            CHAIN,  // chain — "solana:mainnet"
            null,   // authToken — null for fresh authorization
            null,   // features
            null,   // addresses
            null    // signInPayload
        ).get()

        val publicKeyBytes = fullAuthResult.publicKey
        val solanaPublicKey = SolanaPublicKey(publicKeyBytes)
        val address = solanaPublicKey.base58()
        val authToken = fullAuthResult.authToken

        if (authToken != null) {
            saveAuthToken(address, authToken)
        }

        // Reset counter after successful full authorization
        reauthAttemptCount = 0

        Log.d(TAG, "Full authorization successful for ${address.take(8)}...")
        return MwaAuthResult(
            address = address,
            authToken = authToken,
            walletLabel = resolveWalletName(fullAuthResult.walletUriBase),
            walletClientType = resolveWalletClientType(fullAuthResult.walletUriBase),
            publicKeyBytes = publicKeyBytes
        )
    }

    /**
     * Maps a wallet's URI base to a friendly display name.
     *
     * Primary: uses walletUriBase from the MWA authorize response.
     * Fallback: uses the resolved intent package name (for wallets that don't return walletUriBase).
     */
    private fun resolveWalletName(walletUriBase: Uri?): String {
        // Primary: walletUriBase from MWA authorize response
        val host = walletUriBase?.host
        if (host != null) {
            val baseName = WALLET_NAME_MAP[host] ?: host.removeSuffix(".com").removeSuffix(".app")
                .replaceFirstChar { it.uppercase() }
            val name = ensureWalletSuffix(baseName)
            Log.d(TAG, "resolveWalletName: from walletUriBase host=$host -> $name")
            return name
        }

        // Fallback: resolved intent package
        val pkg = lastResolvedWalletPackage
        if (pkg != null) {
            val baseName = WALLET_PACKAGE_MAP[pkg]
            if (baseName != null) {
                val name = ensureWalletSuffix(baseName)
                Log.d(TAG, "resolveWalletName: from package=$pkg -> $name")
                return name
            }
        }

        Log.d(TAG, "resolveWalletName: walletUriBase=$walletUriBase, package=$pkg -> External Wallet")
        return "External Wallet"
    }

    /**
     * Resolves the wallet client type identifier for Privy WalletLoginMetadata.
     * Returns a lowercase string like "phantom", "solflare", etc.
     */
    private fun resolveWalletClientType(walletUriBase: Uri?): String {
        val host = walletUriBase?.host
        if (host != null) {
            WALLET_CLIENT_TYPE_MAP[host]?.let { return it }
        }
        val pkg = lastResolvedWalletPackage
        if (pkg != null) {
            WALLET_PACKAGE_CLIENT_TYPE_MAP[pkg]?.let { return it }
        }
        return "unknown"
    }

    /** Append " Wallet" suffix if not already present (e.g. "Phantom" -> "Phantom Wallet") */
    private fun ensureWalletSuffix(name: String): String {
        return if (name.endsWith("Wallet", ignoreCase = true)) name else "$name Wallet"
    }

    /**
     * Maps generic exceptions from the MWA SDK to typed [MwaError] instances.
     */
    private fun mapExceptionToMwaError(e: Exception): MwaError {
        val message = e.message ?: ""
        val className = e::class.simpleName ?: ""

        return when {
            // JsonRpc20Exception or wallet-specific rejection
            className.contains("JsonRpc20", ignoreCase = true) ||
                className.contains("NotAuthorized", ignoreCase = true) -> {
                val code = try {
                    // Attempt to extract error code via reflection for JsonRpc20Exception
                    e::class.java.getMethod("getCode").invoke(e) as? Int ?: -1
                } catch (_: Exception) {
                    -1
                }
                MwaError.WalletRejected(code, message)
            }

            // User cancelled the wallet interaction
            message.contains("cancel", ignoreCase = true) ||
                message.contains("declined", ignoreCase = true) ||
                message.contains("denied", ignoreCase = true) -> {
                MwaError.UserCancelled
            }

            // Session / association terminated prematurely
            message.contains("session", ignoreCase = true) ||
                message.contains("terminated", ignoreCase = true) ||
                message.contains("disconnect", ignoreCase = true) ||
                className.contains("AssociationError", ignoreCase = true) -> {
                MwaError.SessionTerminated
            }

            // Timeout variants
            e is TimeoutException ||
                e is java.util.concurrent.TimeoutException ||
                message.contains("timeout", ignoreCase = true) -> {
                MwaError.Timeout
            }

            // Activity not found (redundant safety — primary catch is above)
            e is ActivityNotFoundException -> MwaError.NoWalletInstalled

            // Everything else
            else -> MwaError.Unknown(e)
        }
    }

    // ========================================================================
    // Auth token persistence
    // ========================================================================

    private fun saveAuthToken(address: String, token: String) {
        try {
            authTokenPrefs.edit()
                .putString("$AUTH_TOKEN_KEY_PREFIX$address", token)
                .putString(KEY_LAST_USED_ADDRESS, address)
                .apply()
            Log.d(TAG, "Saved MWA auth token for ${address.take(8)}...")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save MWA auth token", e)
        }
    }

    private fun getAuthToken(address: String): String? {
        return try {
            authTokenPrefs.getString("$AUTH_TOKEN_KEY_PREFIX$address", null)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read MWA auth token", e)
            null
        }
    }

    private fun getLastUsedAuthToken(): String? {
        return try {
            val lastAddress = authTokenPrefs.getString(KEY_LAST_USED_ADDRESS, null)
                ?: return null
            getAuthToken(lastAddress)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read last-used MWA auth token", e)
            null
        }
    }

    /**
     * Remove the stored auth token for a specific wallet address.
     */
    fun removeAuthToken(address: String) {
        try {
            authTokenPrefs.edit()
                .remove("$AUTH_TOKEN_KEY_PREFIX$address")
                .apply()
            Log.d(TAG, "Removed MWA auth token for ${address.take(8)}...")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove MWA auth token", e)
        }
    }

    // ========================================================================
    // WalletUriBase persistence
    // ========================================================================

    /**
     * Save walletUriBase keyed by both address and package name.
     * Package-keyed storage prevents cross-contamination when targeting different wallets.
     */
    private fun saveWalletUriBase(address: String, uriBase: String) {
        try {
            val editor = authTokenPrefs.edit()
                .putString("${WALLET_URI_BASE_KEY_PREFIX}$address", uriBase)
            // Also save keyed by package for targeted lookups
            val pkg = lastResolvedWalletPackage
            if (pkg != null) {
                editor.putString("${WALLET_URI_BASE_KEY_PREFIX}pkg_$pkg", uriBase)
                Log.d(TAG, "Saved walletUriBase for ${address.take(8)}... and pkg=$pkg: $uriBase")
            } else {
                Log.d(TAG, "Saved walletUriBase for ${address.take(8)}...: $uriBase")
            }
            editor.apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save walletUriBase", e)
        }
    }

    /**
     * Get stored walletUriBase for a specific wallet package.
     * Only returns a value if it was saved for THIS package — never falls back to
     * another wallet's uriBase, which would cause cross-contamination (e.g., launching
     * Backpack with Phantom's uriPrefix).
     */
    private fun getStoredWalletUriBase(targetPackage: String): String? {
        return try {
            authTokenPrefs.getString("${WALLET_URI_BASE_KEY_PREFIX}pkg_$targetPackage", null)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read walletUriBase for $targetPackage", e)
            null
        }
    }
}

/**
 * Represents an installed MWA-compatible wallet app on the device.
 */
data class InstalledMwaWallet(
    val packageName: String,
    val displayName: String,
    val walletClientType: String
)

private const val KEY_LAST_USED_ADDRESS = "last_used_wallet_address"
private const val WALLET_URI_BASE_KEY_PREFIX = "wallet_uri_base_"
