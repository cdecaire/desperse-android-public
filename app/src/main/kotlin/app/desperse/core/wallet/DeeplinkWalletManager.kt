package app.desperse.core.wallet

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import android.util.Log
import app.desperse.core.util.Base58
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages wallet connections via the Phantom/Solflare deeplink protocol.
 *
 * Protocol overview (Phantom universal links v2):
 * 1. Connect: open wallet deeplink with our X25519 public key
 * 2. Wallet redirects back with encrypted response (wallet address + session token)
 * 3. SignMessage: open wallet deeplink with encrypted message payload
 * 4. Wallet redirects back with encrypted signature
 *
 * Both Phantom and Solflare use the same protocol format.
 */
@Singleton
class DeeplinkWalletManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    companion object {
        private const val TAG = "DeeplinkWalletManager"
        private const val APP_URL = "https://desperse.com"
        private const val REDIRECT_URL = "desperse://wallet-callback"
        private const val CLUSTER = "mainnet-beta"

        /** Base URLs for wallet deeplink protocols */
        val WALLET_DEEPLINK_URLS = mapOf(
            "com.solflare.mobile" to "https://solflare.com/ul/v1",
            "app.phantom" to "https://phantom.app/ul/v1",
        )

        /** Wallets that should use deeplinks instead of MWA */
        val DEEPLINK_PREFERRED_PACKAGES = setOf(
            "com.solflare.mobile",
        )

        private const val PREFS_NAME = "deeplink_wallet_session"
        private const val KEY_PUBLIC_KEY = "publicKey"
        private const val KEY_SECRET_KEY = "secretKey"
        private const val KEY_WALLET_PUB_KEY = "walletPublicKey"
        private const val KEY_SESSION_TOKEN = "sessionToken"
        private const val KEY_CONNECTED_ADDR = "connectedAddress"
        private const val KEY_BASE_URL = "walletBaseUrl"
        private const val KEY_WALLET_PKG = "walletPackage"
        private const val KEY_SIWS_MESSAGE = "pendingSiwsMessage"
        private const val KEY_FLOW_STATE = "flowState"
    }

    /** Tracks where we are in the deeplink connect→sign flow. */
    enum class FlowState { IDLE, AWAITING_CONNECT, AWAITING_SIGN }

    private val sodium = SodiumAndroid()
    private val lazySodium = LazySodiumAndroid(sodium)
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Session state — persisted to SharedPreferences so it survives process death
    private var sessionKeypair: NaClKeyPair? = null
    private var walletPublicKey: ByteArray? = null
    private var sessionToken: String? = null
    private var connectedAddress: String? = null
    private var currentWalletBaseUrl: String? = null
    private var currentWalletPackage: String? = null

    /** Current state of the deeplink flow. */
    var flowState: FlowState = FlowState.IDLE
        private set

    /** SIWS message pending signature. Stored here (not ViewModel) to survive activity recreation. */
    var pendingSiwsMessage: String? = null
        set(value) {
            field = value
            if (sessionKeypair != null) persistSession()
        }

    init {
        restoreSession()
    }

    /** Broadcasts wallet deeplink callback URIs so any ViewModel can subscribe. */
    private val _walletCallbacks = MutableSharedFlow<Uri>(replay = 1, extraBufferCapacity = 1)
    val walletCallbacks: SharedFlow<Uri> = _walletCallbacks.asSharedFlow()

    /** Emit a wallet callback URI (called from MainActivity). */
    fun onWalletCallback(uri: Uri) {
        Log.d(TAG, "onWalletCallback: $uri")
        _walletCallbacks.tryEmit(uri)
    }

    /** Clear the replay buffer so first() won't pick up stale values. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun resetCallbacks() {
        _walletCallbacks.resetReplayCache()
    }

    /** Get the wallet address from the current deeplink session, if connected. */
    fun getConnectedAddress(): String? = connectedAddress

    /** Last error message from handleConnectResponse/handleSignResponse for debugging. */
    var lastError: String? = null
        private set

    /** Whether we're waiting for a sign response (connect already completed). */
    fun isAwaitingSignResponse(): Boolean = connectedAddress != null && pendingSiwsMessage != null

    /** Whether there's any active deeplink session (connect or sign in progress). */
    fun hasActiveSession(): Boolean = sessionKeypair != null

    /**
     * Whether a specific wallet should use deeplinks instead of MWA.
     */
    fun shouldUseDeeplink(packageName: String): Boolean {
        return packageName in DEEPLINK_PREFERRED_PACKAGES
    }

    /**
     * Step 1: Start the connect flow by opening the wallet's connect deeplink.
     *
     * Generates a fresh X25519 keypair for this session and constructs the
     * connect URL with our public key and redirect URI.
     *
     * @param walletPackage The package name of the target wallet.
     * @return Intent to launch, or null if wallet doesn't support deeplinks.
     */
    fun startConnect(walletPackage: String): Intent? {
        val baseUrl = WALLET_DEEPLINK_URLS[walletPackage]
        if (baseUrl == null) {
            Log.w(TAG, "No deeplink URL for package: $walletPackage")
            return null
        }

        // Generate fresh X25519 keypair for this session
        val keypair = generateKeyPair()
        if (keypair == null) {
            Log.e(TAG, "Failed to generate X25519 keypair")
            return null
        }
        sessionKeypair = keypair
        currentWalletBaseUrl = baseUrl
        currentWalletPackage = walletPackage
        flowState = FlowState.AWAITING_CONNECT
        persistSession()

        val dappPublicKey = Base58.encode(keypair.publicKey)

        val url = buildString {
            append("$baseUrl/connect")
            append("?app_url=${urlEncode(APP_URL)}")
            append("&dapp_encryption_public_key=$dappPublicKey")
            append("&redirect_link=${urlEncode(REDIRECT_URL)}")
            append("&cluster=$CLUSTER")
        }

        Log.d(TAG, "Connect deeplink: $url")

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    /**
     * Step 2: Handle the connect response from the wallet's redirect.
     *
     * Decrypts the response to extract the wallet's public key (address),
     * their encryption public key, and the session token for subsequent requests.
     *
     * @param data The URI from the redirect callback.
     * @return The connected wallet address, or null on failure.
     */
    fun handleConnectResponse(data: Uri): String? {
        lastError = null

        val keypair = sessionKeypair
        if (keypair == null) {
            lastError = "No session keypair (session expired?)"
            Log.e(TAG, "handleConnectResponse: $lastError")
            return null
        }

        Log.d(TAG, "handleConnectResponse: URI params=${data.queryParameterNames}")

        // Check for error response
        val errorCode = data.getQueryParameter("errorCode")
        if (errorCode != null) {
            val errorMessage = data.getQueryParameter("errorMessage") ?: "Unknown error"
            lastError = "Wallet returned error: $errorMessage (code=$errorCode)"
            Log.e(TAG, "handleConnectResponse: $lastError")
            return null
        }

        // Each wallet uses its own prefix: phantom_encryption_public_key,
        // solflare_encryption_public_key, etc. Find any param ending with _encryption_public_key.
        val walletPubKeyB58 = data.queryParameterNames
            .firstOrNull { it.endsWith("_encryption_public_key") }
            ?.let { data.getQueryParameter(it) }
        val nonceB58 = data.getQueryParameter("nonce")
        val dataB58 = data.getQueryParameter("data")

        if (walletPubKeyB58 == null || nonceB58 == null || dataB58 == null) {
            lastError = "Missing params in response (pubkey=${walletPubKeyB58 != null}, nonce=${nonceB58 != null}, data=${dataB58 != null})"
            Log.e(TAG, "handleConnectResponse: $lastError")
            return null
        }

        try {
            val walletEncPubKey = Base58.decode(walletPubKeyB58)
            val nonce = Base58.decode(nonceB58)
            val encryptedData = Base58.decode(dataB58)

            walletPublicKey = walletEncPubKey

            val decrypted = naclBoxOpen(encryptedData, nonce, walletEncPubKey, keypair.secretKey)
            if (decrypted == null) {
                lastError = "Failed to decrypt connect response"
                Log.e(TAG, "handleConnectResponse: $lastError")
                return null
            }

            val responseJson = String(decrypted, Charsets.UTF_8)
            Log.d(TAG, "Connect response decrypted: $responseJson")

            val response = json.decodeFromString<ConnectResponse>(responseJson)
            connectedAddress = response.public_key
            sessionToken = response.session
            flowState = FlowState.AWAITING_SIGN
            persistSession()

            Log.d(TAG, "Connected to wallet: ${connectedAddress?.take(8)}...")
            return connectedAddress
        } catch (e: Exception) {
            lastError = "Exception: ${e.message}"
            Log.e(TAG, "handleConnectResponse: $lastError", e)
            return null
        }
    }

    /**
     * Step 3: Start the signMessage flow by opening the wallet's signMessage deeplink.
     *
     * Uses the session established during connect to encrypt the message payload.
     *
     * @param message The message bytes to sign (e.g., SIWS challenge).
     * @return Intent to launch, or null on failure.
     */
    fun startSignMessage(message: ByteArray): Intent? {
        val keypair = sessionKeypair
        val walletPubKey = walletPublicKey
        val session = sessionToken
        val baseUrl = currentWalletBaseUrl

        if (keypair == null || walletPubKey == null || session == null || baseUrl == null) {
            Log.e(TAG, "startSignMessage: Missing session state (keypair=${keypair != null}, walletPubKey=${walletPubKey != null}, session=${session != null})")
            return null
        }

        try {
            val payload = json.encodeToString(SignMessagePayload(
                session = session,
                message = Base58.encode(message),
                display = "utf8"
            ))

            val nonce = generateNonce()
            if (nonce == null) {
                Log.e(TAG, "Failed to generate nonce")
                return null
            }

            val encrypted = naclBox(payload.toByteArray(Charsets.UTF_8), nonce, walletPubKey, keypair.secretKey)
            if (encrypted == null) {
                Log.e(TAG, "Failed to encrypt signMessage payload")
                return null
            }

            val url = buildString {
                append("$baseUrl/signMessage")
                append("?dapp_encryption_public_key=${Base58.encode(keypair.publicKey)}")
                append("&nonce=${Base58.encode(nonce)}")
                append("&redirect_link=${urlEncode(REDIRECT_URL)}")
                append("&payload=${Base58.encode(encrypted)}")
            }

            Log.d(TAG, "SignMessage deeplink constructed (${url.length} chars)")

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        } catch (e: Exception) {
            Log.e(TAG, "Failed to construct signMessage deeplink", e)
            return null
        }
    }

    /**
     * Step 4: Handle the signMessage response from the wallet's redirect.
     *
     * Decrypts the response to extract the signature bytes.
     *
     * @param data The URI from the redirect callback.
     * @return The signature bytes, or null on failure.
     */
    fun handleSignResponse(data: Uri): ByteArray? {
        val keypair = sessionKeypair
        val walletPubKey = walletPublicKey

        if (keypair == null || walletPubKey == null) {
            Log.e(TAG, "handleSignResponse: No session state")
            return null
        }

        // Check for error response
        val errorCode = data.getQueryParameter("errorCode")
        if (errorCode != null) {
            val errorMessage = data.getQueryParameter("errorMessage") ?: "Unknown error"
            Log.e(TAG, "Sign error: code=$errorCode, message=$errorMessage")
            return null
        }

        val nonceB58 = data.getQueryParameter("nonce")
        val dataB58 = data.getQueryParameter("data")

        if (nonceB58 == null || dataB58 == null) {
            Log.e(TAG, "Sign response missing params")
            return null
        }

        try {
            val nonce = Base58.decode(nonceB58)
            val encryptedData = Base58.decode(dataB58)

            val decrypted = naclBoxOpen(encryptedData, nonce, walletPubKey, keypair.secretKey)
            if (decrypted == null) {
                Log.e(TAG, "Failed to decrypt sign response")
                return null
            }

            val responseJson = String(decrypted, Charsets.UTF_8)
            Log.d(TAG, "Sign response decrypted: $responseJson")

            val response = json.decodeFromString<SignMessageResponse>(responseJson)
            val signatureBytes = Base58.decode(response.signature)

            Log.d(TAG, "Got signature (${signatureBytes.size} bytes)")
            return signatureBytes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle sign response", e)
            return null
        }
    }

    /**
     * Get the wallet client type identifier for the currently connected wallet.
     */
    fun getConnectedWalletClientType(): String {
        return when (currentWalletPackage) {
            "app.phantom" -> "phantom"
            "com.solflare.mobile" -> "solflare"
            "com.solanamobile.wallet" -> "seeker"
            else -> "unknown"
        }
    }

    /**
     * Clear all session state. Call after login completes or on error.
     */
    fun clearSession() {
        sessionKeypair = null
        walletPublicKey = null
        sessionToken = null
        connectedAddress = null
        currentWalletBaseUrl = null
        currentWalletPackage = null
        pendingSiwsMessage = null
        lastError = null
        flowState = FlowState.IDLE
        clearPersistedSession()
    }

    // ========================================================================
    // Session persistence (survives process death)
    // ========================================================================

    private fun persistSession() {
        prefs.edit().apply {
            putString(KEY_PUBLIC_KEY, sessionKeypair?.publicKey?.toBase64())
            putString(KEY_SECRET_KEY, sessionKeypair?.secretKey?.toBase64())
            putString(KEY_WALLET_PUB_KEY, walletPublicKey?.toBase64())
            putString(KEY_SESSION_TOKEN, sessionToken)
            putString(KEY_CONNECTED_ADDR, connectedAddress)
            putString(KEY_BASE_URL, currentWalletBaseUrl)
            putString(KEY_WALLET_PKG, currentWalletPackage)
            putString(KEY_SIWS_MESSAGE, pendingSiwsMessage)
            putString(KEY_FLOW_STATE, flowState.name)
            apply()
        }
    }

    private fun restoreSession() {
        val stateStr = prefs.getString(KEY_FLOW_STATE, null) ?: return
        val state = try { FlowState.valueOf(stateStr) } catch (_: Exception) { return }
        if (state == FlowState.IDLE) return

        val pubKeyB64 = prefs.getString(KEY_PUBLIC_KEY, null)
        val secKeyB64 = prefs.getString(KEY_SECRET_KEY, null)
        if (pubKeyB64 == null || secKeyB64 == null) return

        sessionKeypair = NaClKeyPair(
            pubKeyB64.fromBase64(),
            secKeyB64.fromBase64()
        )
        prefs.getString(KEY_WALLET_PUB_KEY, null)?.let { walletPublicKey = it.fromBase64() }
        sessionToken = prefs.getString(KEY_SESSION_TOKEN, null)
        connectedAddress = prefs.getString(KEY_CONNECTED_ADDR, null)
        currentWalletBaseUrl = prefs.getString(KEY_BASE_URL, null)
        currentWalletPackage = prefs.getString(KEY_WALLET_PKG, null)
        pendingSiwsMessage = prefs.getString(KEY_SIWS_MESSAGE, null)
        flowState = state
        Log.d(TAG, "Restored deeplink session: state=$state, wallet=${currentWalletPackage}, addr=${connectedAddress?.take(8)}")
    }

    private fun clearPersistedSession() {
        prefs.edit().clear().apply()
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    // ========================================================================
    // NaCl crypto operations via lazysodium
    // ========================================================================

    private fun generateKeyPair(): NaClKeyPair? {
        return try {
            val publicKey = ByteArray(32)
            val secretKey = ByteArray(32)
            sodium.crypto_box_keypair(publicKey, secretKey)
            NaClKeyPair(publicKey, secretKey)
        } catch (e: Exception) {
            Log.e(TAG, "crypto_box_keypair failed", e)
            null
        }
    }

    private fun generateNonce(): ByteArray? {
        return try {
            val nonce = ByteArray(24) // crypto_box_NONCEBYTES
            sodium.randombytes_buf(nonce, 24)
            nonce
        } catch (e: Exception) {
            Log.e(TAG, "randombytes_buf failed", e)
            null
        }
    }

    /**
     * NaCl box: encrypt message with recipient's public key and our secret key.
     * Returns ciphertext (MAC + encrypted message).
     */
    private fun naclBox(message: ByteArray, nonce: ByteArray, theirPublicKey: ByteArray, ourSecretKey: ByteArray): ByteArray? {
        return try {
            val ciphertext = ByteArray(message.size + 16) // 16 = crypto_box_MACBYTES
            val result = sodium.crypto_box_easy(ciphertext, message, message.size.toLong(), nonce, theirPublicKey, ourSecretKey)
            if (result == 0) ciphertext else null
        } catch (e: Exception) {
            Log.e(TAG, "crypto_box_easy failed", e)
            null
        }
    }

    /**
     * NaCl box open: decrypt ciphertext with sender's public key and our secret key.
     * Returns plaintext message.
     */
    private fun naclBoxOpen(ciphertext: ByteArray, nonce: ByteArray, theirPublicKey: ByteArray, ourSecretKey: ByteArray): ByteArray? {
        return try {
            val plaintext = ByteArray(ciphertext.size - 16) // 16 = crypto_box_MACBYTES
            val result = sodium.crypto_box_open_easy(plaintext, ciphertext, ciphertext.size.toLong(), nonce, theirPublicKey, ourSecretKey)
            if (result == 0) plaintext else null
        } catch (e: Exception) {
            Log.e(TAG, "crypto_box_open_easy failed", e)
            null
        }
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    // ========================================================================
    // Protocol DTOs
    // ========================================================================

    private data class NaClKeyPair(val publicKey: ByteArray, val secretKey: ByteArray)

    @Serializable
    private data class ConnectResponse(
        val public_key: String,
        val session: String
    )

    @Serializable
    private data class SignMessagePayload(
        val session: String,
        val message: String,   // Base58-encoded message
        val display: String = "utf8"
    )

    @Serializable
    private data class SignMessageResponse(
        val signature: String  // Base58-encoded signature
    )
}
