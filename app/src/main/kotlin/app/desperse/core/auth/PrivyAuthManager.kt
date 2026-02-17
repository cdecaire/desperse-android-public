package app.desperse.core.auth

import android.app.Application
import android.os.Trace
import android.util.Base64
import android.util.Log
import app.desperse.BuildConfig
import app.desperse.core.util.Base58
import app.desperse.data.model.User
import io.privy.sdk.Privy
import io.privy.sdk.PrivyConfig
import io.privy.auth.AuthState as PrivyAuthState
import io.privy.auth.PrivyUser
import io.privy.auth.siwe.WalletLoginMetadata
import io.privy.auth.siws.SiwsMessageParams
import io.privy.logging.PrivyLogLevel
import io.privy.wallet.WalletClientType
import io.privy.auth.LinkedAccount
import io.privy.wallet.solana.EmbeddedSolanaWallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages authentication via Privy SDK.
 *
 * Privy SDK provides:
 * - Email OTP login
 * - SMS OTP login
 * - OAuth (Google, Twitter, Discord)
 * - SIWS (Sign In With Solana) via MWA external wallets
 * - Embedded Solana wallet creation and signing
 */
@Singleton
class PrivyAuthManager @Inject constructor(
    private val application: Application,
    private val tokenStorage: TokenStorage
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotReady)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var privy: Privy? = null
    private var isInitialized = false
    private val initLock = Any()

    // Store the authenticated Privy user for wallet operations
    private var authenticatedPrivyUser: PrivyUser? = null

    // Stored between generateSiwsMessage() and loginWithSiws() calls
    private var pendingSiwsParams: SiwsMessageParams? = null

    // Prevents concurrent embedded wallet creation (race from duplicate initAuth calls)
    private val walletCreationMutex = Mutex()

    // MWA wallet address for SIWS users (who don't have a Privy embedded wallet)
    private var siwsWalletAddress: String? = null

    // Wallet client type for SIWS logins (e.g., "phantom", "solflare")
    private var siwsWalletClientType: String? = null

    companion object {
        private const val TAG = "PrivyAuthManager"
        private const val APP_DOMAIN = "desperse.com"
        private const val APP_URI = "https://desperse.com"
    }

    /**
     * Initialize Privy SDK. Must be called on main thread.
     * Safe to call multiple times - will only initialize once.
     */
    suspend fun initialize() {
        // Prevent double initialization
        synchronized(initLock) {
            if (isInitialized) {
                Log.d(TAG, "Privy already initialized, skipping")
                return
            }
            isInitialized = true
        }

        _authState.value = AuthState.Loading

        try {
            // Initialize Privy on main thread
            withContext(Dispatchers.Main) {
                Trace.beginSection("Privy.init")
                privy = Privy.init(
                    context = application,
                    config = PrivyConfig(
                        appId = BuildConfig.PRIVY_APP_ID,
                        appClientId = BuildConfig.PRIVY_APP_CLIENT_ID,
                        logLevel = if (BuildConfig.DEBUG) PrivyLogLevel.VERBOSE else PrivyLogLevel.NONE
                    )
                )
                Trace.endSection() // Privy.init
            }

            // Check if user is already logged in
            Trace.beginSection("Privy.checkAuthState")
            checkAuthState()
            Trace.endSection() // Privy.checkAuthState
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Privy", e)
            _authState.value = AuthState.Error(e.message ?: "Failed to initialize")
            // Reset so we can try again
            synchronized(initLock) {
                isInitialized = false
            }
        }
    }

    /**
     * Check current authentication state.
     * Privy SDK may take a moment to restore session from storage.
     * We collect the authState Flow to properly wait for session restoration.
     */
    private suspend fun checkAuthState() {
        try {
            val p = privy ?: return

            // p.authState is a StateFlow - we need to collect it
            val authStateFlow = p.authState
            var currentState = authStateFlow.value

            Log.d(TAG, "checkAuthState: Starting, initial state = ${currentState::class.simpleName}")

            // Wait for Privy to transition from NotReady to a terminal state
            // Use longer timeout (10 seconds) with increasing delays
            val maxWaitMs = 10_000L
            val startTime = System.currentTimeMillis()
            var delayMs = 100L

            while (currentState is PrivyAuthState.NotReady) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= maxWaitMs) {
                    Log.d(TAG, "checkAuthState: Timeout after ${elapsed}ms, state still NotReady")
                    break
                }
                Log.d(TAG, "checkAuthState: Waiting for ready state (elapsed: ${elapsed}ms)")
                kotlinx.coroutines.delay(delayMs)
                // Increase delay up to 500ms to reduce polling frequency
                delayMs = minOf(delayMs + 50, 500L)
                currentState = authStateFlow.value
            }

            Log.d(TAG, "checkAuthState: Final state = ${currentState::class.simpleName}")

            when (currentState) {
                is PrivyAuthState.Authenticated -> {
                    val user = currentState.user
                    authenticatedPrivyUser = user  // Store for wallet operations
                    Log.d(TAG, "checkAuthState: User authenticated: ${user.id}")

                    val token = getAccessToken()
                    if (token != null) {
                        Log.d(TAG, "checkAuthState: Saving access token")
                        tokenStorage.saveAccessToken(token)
                    } else {
                        Log.w(TAG, "checkAuthState: No access token available - API calls may fail!")
                    }
                    _authState.value = AuthState.Authenticated(toUser(user))
                    Log.d(TAG, "User already authenticated: ${user.id}")
                }
                is PrivyAuthState.Unauthenticated -> {
                    _authState.value = AuthState.Unauthenticated
                    Log.d(TAG, "No authenticated user")
                }
                is PrivyAuthState.NotReady -> {
                    _authState.value = AuthState.Unauthenticated
                    Log.d(TAG, "Privy not ready after timeout, treating as unauthenticated")
                }
                else -> {
                    _authState.value = AuthState.Unauthenticated
                    Log.d(TAG, "Unknown Privy state: ${currentState::class.simpleName}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking auth state", e)
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * Login with OAuth provider (Google, Twitter, Discord).
     * Opens the provider's login page in a browser and handles the redirect.
     */
    suspend fun loginWithOAuth(provider: OAuthProvider): Result<User> {
        return try {
            _authState.value = AuthState.Loading

            val p = privy ?: return Result.failure(Exception("Privy not initialized"))

            val oAuthProvider = when (provider) {
                OAuthProvider.GOOGLE -> io.privy.auth.oAuth.OAuthProvider.Google
                OAuthProvider.TWITTER -> io.privy.auth.oAuth.OAuthProvider.Twitter
                OAuthProvider.DISCORD -> io.privy.auth.oAuth.OAuthProvider.Discord
            }

            val result = p.oAuth.login(oAuthProvider, "desperse://auth")
            result.fold(
                onSuccess = { user ->
                    authenticatedPrivyUser = user
                    Log.d(TAG, "OAuth login successful, user=${user.id}")

                    val token = getAccessToken()
                    if (token != null) {
                        Log.d(TAG, "OAuth: Saving access token (length=${token.length})")
                        tokenStorage.saveAccessToken(token)
                    } else {
                        Log.e(TAG, "OAuth: CRITICAL - No access token available after login!")
                    }
                    val appUser = toUser(user)
                    _authState.value = AuthState.Authenticated(appUser)
                    Result.success(appUser)
                },
                onFailure = { error ->
                    Log.e(TAG, "OAuth login failed: ${error.message}", error)
                    _authState.value = AuthState.Error(error.message ?: "OAuth login failed")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "OAuth login exception", e)
            _authState.value = AuthState.Error(e.message ?: "OAuth login failed")
            Result.failure(e)
        }
    }

    /**
     * Send email verification code.
     */
    suspend fun sendEmailCode(email: String): Result<Unit> {
        Log.d(TAG, "sendEmailCode called for: $email")
        return try {
            val p = privy
            if (p == null) {
                Log.e(TAG, "Privy is null - not initialized")
                return Result.failure(Exception("Privy not initialized"))
            }
            Log.d(TAG, "Calling privy.email.sendCode...")
            p.email.sendCode(email)
            Log.d(TAG, "Email code sent successfully to $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email code: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verify email code and complete login.
     */
    suspend fun verifyEmailCode(email: String, code: String): Result<User> {
        return try {
            _authState.value = AuthState.Loading

            val p = privy ?: return Result.failure(Exception("Privy not initialized"))
            val result = p.email.loginWithCode(code = code, email = email)

            result.fold(
                onSuccess = { user ->
                    authenticatedPrivyUser = user  // Store for wallet operations
                    Log.d(TAG, "verifyEmailCode: Login successful, user=${user.id}")

                    val token = getAccessToken()
                    if (token != null) {
                        Log.d(TAG, "verifyEmailCode: Saving access token (length=${token.length})")
                        tokenStorage.saveAccessToken(token)
                        // Verify the token was saved
                        val savedToken = tokenStorage.getCachedAccessToken()
                        Log.d(TAG, "verifyEmailCode: Token cached=${savedToken != null}, length=${savedToken?.length ?: 0}")
                    } else {
                        Log.e(TAG, "verifyEmailCode: CRITICAL - No access token available after login!")
                    }
                    val appUser = toUser(user)
                    _authState.value = AuthState.Authenticated(appUser)
                    Log.d(TAG, "Login successful: ${user.id}")
                    Result.success(appUser)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                    Log.e(TAG, "Login failed", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Login failed")
            Log.e(TAG, "Login exception", e)
            Result.failure(e)
        }
    }

    /**
     * Send SMS verification code.
     */
    suspend fun sendSmsCode(phoneNumber: String): Result<Unit> {
        return try {
            val p = privy ?: return Result.failure(Exception("Privy not initialized"))
            p.sms.sendCode(phoneNumber)
            Log.d(TAG, "SMS code sent to $phoneNumber")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS code", e)
            Result.failure(e)
        }
    }

    /**
     * Verify SMS code and complete login.
     */
    suspend fun verifySmsCode(phoneNumber: String, code: String): Result<User> {
        return try {
            _authState.value = AuthState.Loading

            val p = privy ?: return Result.failure(Exception("Privy not initialized"))
            val result = p.sms.loginWithCode(code = code, phoneNumber = phoneNumber)

            result.fold(
                onSuccess = { user ->
                    val token = getAccessToken()
                    if (token != null) {
                        tokenStorage.saveAccessToken(token)
                    }
                    val appUser = toUser(user)
                    _authState.value = AuthState.Authenticated(appUser)
                    Result.success(appUser)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Login failed")
            Result.failure(e)
        }
    }

    /**
     * Get or create embedded Solana wallet.
     *
     * Always checks Privy SDK's current authState for the freshest user object,
     * since the cached `authenticatedPrivyUser` from login time may not include
     * embedded wallets created in a previous session (SIWS login returns a user
     * snapshot that can be stale w.r.t. embedded wallets).
     */
    suspend fun getOrCreateEmbeddedWallet(): Result<String> = walletCreationMutex.withLock {
        try {
            val p = privy ?: return@withLock Result.failure(Exception("Privy not initialized"))

            // Always get the freshest user from Privy SDK state — the cached
            // authenticatedPrivyUser from SIWS login may not include embedded
            // wallets that were created in a previous session.
            val state = p.authState.value
            Log.d(TAG, "getOrCreateEmbeddedWallet: Privy authState = ${state::class.simpleName}")

            val user = if (state is PrivyAuthState.Authenticated) {
                state.user.also { authenticatedPrivyUser = it }
            } else {
                // Fall back to cached user if SDK state isn't Authenticated yet
                authenticatedPrivyUser ?: run {
                    Log.e(TAG, "getOrCreateEmbeddedWallet: No authenticated user available")
                    return@withLock Result.failure(Exception("Not authenticated - Privy state: ${state::class.simpleName}"))
                }
            }

            // Check for existing Solana wallet
            val existingWallet = user.embeddedSolanaWallets.firstOrNull()
            if (existingWallet != null) {
                Log.d(TAG, "Found existing Solana wallet: ${existingWallet.address}")
                return@withLock Result.success(existingWallet.address)
            }

            // Create new Solana wallet
            Log.d(TAG, "No existing wallet, creating new Solana wallet...")
            val result = user.createSolanaWallet()
            result.fold(
                onSuccess = { wallet ->
                    Log.d(TAG, "Created new Solana wallet: ${wallet.address}")
                    // Refresh cached user from SDK to include the new wallet
                    val freshState = p.authState.value
                    if (freshState is PrivyAuthState.Authenticated) {
                        authenticatedPrivyUser = freshState.user
                    }
                    Result.success(wallet.address)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to create Solana wallet: ${error.message}", error)
                    // Wallet may already exist (stale user cache from SDK) —
                    // re-check SDK state before giving up
                    val retryState = p.authState.value
                    if (retryState is PrivyAuthState.Authenticated) {
                        val retryWallet = retryState.user.embeddedSolanaWallets.firstOrNull()
                        if (retryWallet != null) {
                            Log.d(TAG, "Found existing wallet on retry: ${retryWallet.address}")
                            authenticatedPrivyUser = retryState.user
                            return@withLock Result.success(retryWallet.address)
                        }
                    }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting/creating wallet", e)
            Result.failure(e)
        }
    }

    /**
     * Sign a serialized V0 VersionedTransaction.
     *
     * CRITICAL: Uses Privy's transaction-aware signing API.
     *
     * @param unsignedTxBase64 Serialized V0 VersionedTransaction (base64)
     * @return signedTxBase64 Fully signed transaction ready for broadcast (base64)
     */
    suspend fun signTransaction(unsignedTxBase64: String): Result<String> {
        return try {
            val p = privy ?: return Result.failure(Exception("Privy not initialized"))

            // Use stored user or get from Privy state
            val user = authenticatedPrivyUser ?: run {
                val state = p.authState.value
                if (state is PrivyAuthState.Authenticated) {
                    state.user.also { authenticatedPrivyUser = it }
                } else {
                    return Result.failure(Exception("Not authenticated"))
                }
            }

            val wallet = user.embeddedSolanaWallets.firstOrNull()
                ?: return Result.failure(Exception("No embedded Solana wallet"))

            Log.d(TAG, "Signing transaction with wallet: ${wallet.address}")

            // Decode base64 to bytes for signTransaction
            val txBytes = Base64.decode(unsignedTxBase64, Base64.NO_WRAP)

            // Use the wallet provider to sign the transaction
            val result = wallet.provider.signTransaction(txBytes)
            result.fold(
                onSuccess = { signedTxBase64 ->
                    Log.d(TAG, "Transaction signed successfully")
                    Result.success(signedTxBase64)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to sign transaction", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception signing transaction", e)
            Result.failure(e)
        }
    }

    /**
     * Sign a message with the embedded wallet.
     * Uses the ByteArray API as the String variant is deprecated.
     */
    suspend fun signMessage(message: String): Result<String> {
        return try {
            val p = privy ?: return Result.failure(Exception("Privy not initialized"))

            // Use stored user or get from Privy state
            val user = authenticatedPrivyUser ?: run {
                val state = p.authState.value
                if (state is PrivyAuthState.Authenticated) {
                    state.user.also { authenticatedPrivyUser = it }
                } else {
                    return Result.failure(Exception("Not authenticated"))
                }
            }

            val wallet = user.embeddedSolanaWallets.firstOrNull()
                ?: return Result.failure(Exception("No embedded Solana wallet"))

            // Convert message to bytes for the non-deprecated API
            val messageBytes = message.toByteArray(Charsets.UTF_8)

            Log.d(TAG, "Signing message (${messageBytes.size} bytes)")
            val result = wallet.provider.signMessage(messageBytes)
            result.fold(
                onSuccess = { signature ->
                    Log.d(TAG, "Message signed successfully")
                    Result.success(signature)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to sign message", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception signing message", e)
            Result.failure(e)
        }
    }

    /**
     * Get embedded wallet address.
     */
    fun getEmbeddedWalletAddress(): String? {
        return try {
            // Try stored user first
            authenticatedPrivyUser?.embeddedSolanaWallets?.firstOrNull()?.address
                ?: run {
                    // Fall back to Privy state
                    val p = privy ?: return null
                    val state = p.authState.value
                    if (state is PrivyAuthState.Authenticated) {
                        state.user.embeddedSolanaWallets.firstOrNull()?.address
                    } else {
                        null
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting wallet address", e)
            null
        }
    }

    /**
     * Get user info needed for backend auth initialization.
     * Returns wallet address (required) and optional email.
     *
     * For SIWS users, the wallet address comes from the MWA authorization
     * (stored in siwsWalletAddress) since they don't have a Privy embedded wallet.
     */
    fun getAuthInitInfo(): AuthInitInfo? {
        return try {
            val user = authenticatedPrivyUser ?: run {
                val p = privy ?: run {
                    Log.w(TAG, "getAuthInitInfo: Privy not initialized")
                    return null
                }
                val state = p.authState.value
                if (state is PrivyAuthState.Authenticated) {
                    Log.d(TAG, "getAuthInitInfo: Using user from Privy state")
                    state.user
                } else {
                    Log.w(TAG, "getAuthInitInfo: Not authenticated, state=${state::class.simpleName}")
                    null
                }
            } ?: run {
                Log.w(TAG, "getAuthInitInfo: No user available")
                return null
            }

            val wallets = user.embeddedSolanaWallets
            Log.d(TAG, "getAuthInitInfo: User has ${wallets.size} embedded Solana wallets")

            // Try embedded wallet first, then fall back to SIWS MWA wallet address
            val walletAddress = wallets.firstOrNull()?.address ?: siwsWalletAddress
            if (walletAddress == null) {
                Log.w(TAG, "getAuthInitInfo: No wallet address found (embedded or SIWS)")
                return null
            }

            Log.d(TAG, "getAuthInitInfo: wallet=$walletAddress (siws=${siwsWalletAddress != null})")
            AuthInitInfo(
                walletAddress = walletAddress,
                email = null,
                isSiwsLogin = siwsWalletAddress != null,
                siwsWalletAddress = siwsWalletAddress,
                siwsWalletClientType = siwsWalletClientType
            )
        } catch (e: Exception) {
            Log.e(TAG, "getAuthInitInfo: Exception", e)
            null
        }
    }

    /**
     * Data class holding info needed for backend auth initialization.
     */
    data class AuthInitInfo(
        val walletAddress: String,
        val email: String? = null,
        val isSiwsLogin: Boolean = false,
        val siwsWalletAddress: String? = null,
        val siwsWalletClientType: String? = null
    )

    /**
     * Get current access token for API calls.
     */
    suspend fun getAccessToken(): String? {
        return try {
            // Try stored user first
            val user = authenticatedPrivyUser ?: run {
                val p = privy ?: run {
                    Log.w(TAG, "getAccessToken: Privy not initialized")
                    return null
                }
                val state = p.authState.value
                if (state is PrivyAuthState.Authenticated) {
                    state.user
                } else {
                    Log.w(TAG, "getAccessToken: Not authenticated, state=${state::class.simpleName}")
                    null
                }
            }

            if (user == null) {
                Log.w(TAG, "getAccessToken: No user available")
                return null
            }

            val result = user.getAccessToken()
            result.fold(
                onSuccess = { token ->
                    Log.d(TAG, "getAccessToken: Got token (length=${token.length})")
                    token
                },
                onFailure = { error ->
                    Log.e(TAG, "getAccessToken: Failed to get token from Privy", error)
                    null
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "getAccessToken: Exception", e)
            null
        }
    }

    /**
     * Generate a SIWS challenge message via Privy SDK.
     * Must be called before loginWithSiws(). The params are stored internally.
     *
     * @param walletAddress Base58-encoded Solana wallet address from MWA.
     * @return The challenge message string to be signed by the wallet.
     */
    suspend fun generateSiwsMessage(walletAddress: String): Result<String> {
        return try {
            val p = privy ?: return Result.failure(Exception("Privy not initialized"))

            val params = SiwsMessageParams(
                appDomain = APP_DOMAIN,
                appUri = APP_URI,
                walletAddress = walletAddress
            )
            pendingSiwsParams = params

            Log.d(TAG, "Generating SIWS message for ${walletAddress.take(8)}...")
            val result = p.siws.generateMessage(params)
            result.fold(
                onSuccess = { message ->
                    Log.d(TAG, "SIWS message generated (length=${message.length})")
                    Result.success(message)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to generate SIWS message", error)
                    pendingSiwsParams = null
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception generating SIWS message", e)
            pendingSiwsParams = null
            Result.failure(e)
        }
    }

    /**
     * Complete SIWS login via Privy SDK after the wallet has signed the challenge.
     *
     * @param message The challenge message that was signed.
     * @param signature The Base64-encoded ed25519 signature from the wallet.
     * @param walletClientType Lowercase wallet identifier (e.g., "phantom", "solflare").
     * @return The authenticated Privy user mapped to the app User model.
     */
    suspend fun loginWithSiws(
        message: String,
        signature: String,
        walletClientType: String
    ): Result<User> {
        return try {
            _authState.value = AuthState.Loading

            val p = privy ?: return Result.failure(Exception("Privy not initialized"))
            val params = pendingSiwsParams
                ?: return Result.failure(Exception("No pending SIWS params - call generateSiwsMessage first"))

            // Store the MWA wallet address and client type before login (SIWS users don't have embedded wallets)
            siwsWalletAddress = params.walletAddress
            siwsWalletClientType = walletClientType

            val metadata = WalletLoginMetadata(
                walletClientType = WalletClientType.Other,
                connectorType = walletClientType
            )

            Log.d(TAG, "SIWS login: wallet=${params.walletAddress.take(8)}..., clientType=$walletClientType")
            val result = p.siws.login(message, signature, params, metadata)

            pendingSiwsParams = null // Clean up regardless of outcome

            result.fold(
                onSuccess = { user ->
                    authenticatedPrivyUser = user
                    Log.d(TAG, "SIWS login successful, user=${user.id}")

                    val token = getAccessToken()
                    if (token != null) {
                        tokenStorage.saveAccessToken(token)
                    } else {
                        Log.e(TAG, "SIWS: No access token available after login")
                    }
                    val appUser = toUser(user)
                    _authState.value = AuthState.Authenticated(appUser)
                    Result.success(appUser)
                },
                onFailure = { error ->
                    Log.e(TAG, "SIWS login failed: ${error.message}", error)
                    siwsWalletAddress = null
                    siwsWalletClientType = null
                    _authState.value = AuthState.Error(error.message ?: "Wallet login failed")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "SIWS login exception", e)
            pendingSiwsParams = null
            siwsWalletAddress = null
            siwsWalletClientType = null
            _authState.value = AuthState.Error(e.message ?: "Wallet login failed")
            Result.failure(e)
        }
    }

    /**
     * Link an external Solana wallet to the current Privy account via SIWS.
     * Unlike loginWithSiws(), this does NOT change auth state — it adds a linked account.
     *
     * @param walletAddress Base58-encoded Solana wallet address.
     * @param message The SIWS challenge message that was signed.
     * @param signatureBase64 The Base64-encoded ed25519 signature.
     * @param walletClientType Lowercase wallet identifier (e.g., "phantom", "solflare").
     */
    suspend fun linkWithSiws(
        walletAddress: String,
        message: String,
        signatureBase64: String,
        walletClientType: String
    ): Result<Unit> {
        return try {
            val p = privy ?: return Result.failure(Exception("Privy not initialized"))
            val params = pendingSiwsParams
                ?: return Result.failure(Exception("No pending SIWS params - call generateSiwsMessage first"))

            val metadata = WalletLoginMetadata(
                walletClientType = WalletClientType.Other,
                connectorType = walletClientType
            )

            Log.d(TAG, "SIWS link: wallet=${walletAddress.take(8)}..., clientType=$walletClientType")
            val result = p.siws.link(message, signatureBase64, params, metadata)

            pendingSiwsParams = null

            result.fold(
                onSuccess = { user ->
                    authenticatedPrivyUser = user
                    Log.d(TAG, "SIWS link successful, user=${user.id}")

                    // Refresh access token — Privy may issue a new one after link
                    val token = getAccessToken()
                    if (token != null) {
                        tokenStorage.saveAccessToken(token)
                        Log.d(TAG, "SIWS link: Refreshed access token (length=${token.length})")
                    } else {
                        Log.w(TAG, "SIWS link: Could not refresh access token after link")
                    }

                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "SIWS link failed: ${error.message}", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "SIWS link exception", e)
            pendingSiwsParams = null
            Result.failure(e)
        }
    }

    /**
     * Unlink an external Solana wallet from the current Privy account.
     *
     * @param address Base58-encoded wallet address to unlink.
     */
    suspend fun unlinkWallet(address: String): Result<Unit> {
        return try {
            val p = privy ?: return Result.failure(Exception("Privy not initialized"))

            Log.d(TAG, "Unlinking wallet ${address.take(8)}...")
            val result = p.siws.unlink(address)
            result.fold(
                onSuccess = { user ->
                    authenticatedPrivyUser = user
                    Log.d(TAG, "Successfully unlinked wallet ${address.take(8)}...")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to unlink wallet ${address.take(8)}...", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception unlinking wallet", e)
            Result.failure(e)
        }
    }

    /**
     * Get linked accounts for the current user.
     */
    fun getLinkedAccounts(): List<LinkedAccount> {
        val user = authenticatedPrivyUser ?: run {
            val p = privy ?: return emptyList()
            val state = p.authState.value
            if (state is PrivyAuthState.Authenticated) state.user else return emptyList()
        }
        return user.linkedAccounts
    }

    /**
     * Link an OAuth account to the current user.
     */
    suspend fun linkOAuth(provider: OAuthProvider): Result<Unit> {
        return try {
            val p = privy ?: return Result.failure(Exception("Privy not initialized"))

            val oAuthProvider = when (provider) {
                OAuthProvider.GOOGLE -> io.privy.auth.oAuth.OAuthProvider.Google
                OAuthProvider.TWITTER -> io.privy.auth.oAuth.OAuthProvider.Twitter
                OAuthProvider.DISCORD -> io.privy.auth.oAuth.OAuthProvider.Discord
            }

            Log.d(TAG, "Linking ${provider.id} account...")
            val result = p.oAuth.link(oAuthProvider, "desperse://auth")
            result.fold(
                onSuccess = { user ->
                    authenticatedPrivyUser = user
                    Log.d(TAG, "Successfully linked ${provider.id}")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to link ${provider.id}", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception linking ${provider.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Unlink an OAuth account from the current user.
     */
    suspend fun unlinkOAuth(provider: OAuthProvider, subject: String): Result<Unit> {
        return try {
            val p = privy ?: return Result.failure(Exception("Privy not initialized"))

            val oAuthProvider = when (provider) {
                OAuthProvider.GOOGLE -> io.privy.auth.oAuth.OAuthProvider.Google
                OAuthProvider.TWITTER -> io.privy.auth.oAuth.OAuthProvider.Twitter
                OAuthProvider.DISCORD -> io.privy.auth.oAuth.OAuthProvider.Discord
            }

            Log.d(TAG, "Unlinking ${provider.id} account...")
            val result = p.oAuth.unlink(oAuthProvider, subject)
            result.fold(
                onSuccess = { user ->
                    authenticatedPrivyUser = user
                    Log.d(TAG, "Successfully unlinked ${provider.id}")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to unlink ${provider.id}", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception unlinking ${provider.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Logout and clear session.
     */
    suspend fun logout() {
        Log.d(TAG, "logout() called")
        try {
            privy?.logout()
            Log.d(TAG, "Privy logout completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during Privy logout", e)
        }
        authenticatedPrivyUser = null
        siwsWalletAddress = null
        siwsWalletClientType = null
        pendingSiwsParams = null
        tokenStorage.clearTokens()
        _authState.value = AuthState.Unauthenticated
        Log.d(TAG, "Logged out - state set to Unauthenticated")
    }

    /**
     * Convert Privy user to app User model.
     * For SIWS users, falls back to siwsWalletAddress since they lack embedded wallets.
     */
    private fun toUser(privyUser: PrivyUser): User {
        val walletAddress = privyUser.embeddedSolanaWallets.firstOrNull()?.address
            ?: siwsWalletAddress

        return User(
            id = privyUser.id,
            slug = privyUser.id.take(8), // Temporary slug from ID
            displayName = null,
            bio = null,
            avatarUrl = null,
            headerUrl = null,
            walletAddress = walletAddress,
            isVerified = false
        )
    }
}

enum class OAuthProvider(val id: String) {
    GOOGLE("google"),
    TWITTER("twitter"),
    DISCORD("discord")
}
