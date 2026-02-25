package app.desperse.ui.screens.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.auth.AuthState
import app.desperse.core.auth.OAuthProvider
import app.desperse.core.auth.PrivyAuthManager
import app.desperse.core.wallet.DeeplinkWalletManager
import app.desperse.core.wallet.InstalledMwaWallet
import app.desperse.core.wallet.MwaAuthResult
import app.desperse.core.wallet.MwaError
import app.desperse.core.wallet.MwaManager
import app.desperse.core.wallet.WalletPreferences
import app.desperse.core.wallet.userFacingMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object MwaConnecting : LoginUiState()
    object MwaSigning : LoginUiState()
    object CodeSent : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

sealed class LoginEvent {
    object LoginSuccess : LoginEvent()
    data class ShowError(val message: String) : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val privyAuthManager: PrivyAuthManager,
    private val mwaManager: MwaManager,
    private val deeplinkWalletManager: DeeplinkWalletManager,
    private val walletPreferences: WalletPreferences
) : ViewModel() {
    companion object {
        private const val TAG = "LoginViewModel"
        private const val SOLFLARE_PACKAGE = "com.solflare.mobile"
    }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events = _events.asSharedFlow()

    val authState: StateFlow<AuthState> = privyAuthManager.authState

    private var currentEmail: String = ""

    /** Whether any MWA-compatible wallet app (Phantom, Solflare, etc.) is installed. */
    val isMwaAvailable: Boolean get() = mwaManager.isAvailable()

    /** Whether this is a Solana Mobile device (Saga/Seeker) with Seed Vault installed. */
    val isSeekerDevice: Boolean get() = mwaManager.isSeekerDevice()

    /** List of installed MWA wallet apps (excludes system services like Seed Vault). */
    val installedWallets: List<InstalledMwaWallet> get() = mwaManager.getInstalledWallets()

    init {
        // Watch for auth state changes - navigate when authenticated
        viewModelScope.launch {
            privyAuthManager.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        Log.d(TAG, "Auth state changed to Authenticated, emitting LoginSuccess")
                        _events.emit(LoginEvent.LoginSuccess)
                    }
                    is AuthState.Error -> {
                        _uiState.value = LoginUiState.Error(state.message)
                    }
                    is AuthState.Unauthenticated, is AuthState.NotReady -> {
                        _uiState.value = LoginUiState.Idle
                    }
                    else -> {}
                }
            }
        }
    }

    fun sendEmailCode(email: String) {
        Log.d(TAG, "sendEmailCode called with: $email")

        if (email.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter an email address")
            return
        }

        currentEmail = email
        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            privyAuthManager.sendEmailCode(email).fold(
                onSuccess = {
                    Log.d(TAG, "Email code sent successfully")
                    _uiState.value = LoginUiState.CodeSent
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to send email code: ${error.message}")
                    _uiState.value = LoginUiState.Error(error.message ?: "Failed to send code")
                }
            )
        }
    }

    fun verifyEmailCode(code: String) {
        if (code.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter the verification code")
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            privyAuthManager.verifyEmailCode(currentEmail, code).fold(
                onSuccess = { user ->
                    Log.d(TAG, "Login successful for user: ${user.id}")
                    // Auth state collector will handle the success event
                },
                onFailure = { error ->
                    Log.e(TAG, "Login failed: ${error.message}")
                    _uiState.value = LoginUiState.Error(error.message ?: "Invalid code")
                }
            )
        }
    }

    fun loginWithGoogle() {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            privyAuthManager.loginWithOAuth(OAuthProvider.GOOGLE).onFailure { error ->
                Log.e(TAG, "Google login failed: ${error.message}")
                _uiState.value = LoginUiState.Error(error.message ?: "Google login failed")
            }
            // Success is handled by authState collector in init block
        }
    }

    fun loginWithX() {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            privyAuthManager.loginWithOAuth(OAuthProvider.TWITTER).onFailure { error ->
                Log.e(TAG, "X login failed: ${error.message}")
                _uiState.value = LoginUiState.Error(error.message ?: "X login failed")
            }
            // Success is handled by authState collector in init block
        }
    }

    /**
     * Initiate login via MWA (Mobile Wallet Adapter) using Privy native SIWS.
     *
     * Attempts a single-session flow (one wallet redirect) via authorizeAndSignMessage.
     * If the Privy challenge generation fails from background (foreground requirement),
     * automatically falls back to a two-step flow using the captured auth result.
     *
     * Single-session flow (best case — one wallet redirect):
     * 1. MWA session: authorize → messageProvider generates SIWS → sign → done
     * 2. Wait for foreground → privy.siws.login()
     *
     * Two-step fallback (if Privy needs foreground for challenge generation):
     * 1. MWA session 1: authorize → messageProvider fails → auth result captured
     * 2. Back in foreground: privy.siws.generateMessage()
     * 3. MWA session 2: reauthorize + sign
     * 4. privy.siws.login()
     */
    fun loginWithMwa(
        activity: Activity,
        targetPackage: String? = null,
        fallbackToDeeplinkOnMwaFailure: Boolean = false
    ) {
        _uiState.value = LoginUiState.MwaConnecting

        viewModelScope.launch {
            Log.d(TAG, "SIWS: Attempting single-session authorize+sign")

            // Capture the SIWS message from the messageProvider so we can pass the
            // exact same message (with matching nonce) to loginWithSiws later.
            var capturedSiwsMessage: String? = null

            val result = mwaManager.authorizeAndSignMessage(activity, targetPackage) { address ->
                Log.d(TAG, "SIWS: messageProvider called for ${address.take(8)}...")
                val message = privyAuthManager.generateSiwsMessage(address).getOrThrow()
                capturedSiwsMessage = message
                Log.d(TAG, "SIWS: Got Privy challenge in-session (length=${message.length})")
                message.toByteArray(Charsets.UTF_8)
            }

            result.fold(
                onSuccess = { (authResult, signatureBytes) ->
                    val siwsMessage = capturedSiwsMessage!!
                    Log.d(TAG, "SIWS: Single-session succeeded for ${authResult.address.take(8)}...")

                    if (targetPackage != null) {
                        walletPreferences.setActiveWalletPackage(targetPackage)
                    }

                    waitForForeground()
                    completeSiwsLogin(authResult, siwsMessage, signatureBytes)
                },
                onFailure = { error ->
                    if (error is MwaError.MessageProviderFailed) {
                        // Authorization succeeded but SIWS generation failed (likely foreground issue).
                        // Fall back to two-step: generate SIWS in foreground, then sign in second session.
                        Log.d(TAG, "SIWS: Falling back to two-step (messageProvider failed: ${error.cause.message})")
                        loginWithMwaTwoStep(activity, error.authResult, targetPackage)
                    } else {
                        if (
                            fallbackToDeeplinkOnMwaFailure &&
                            !targetPackage.isNullOrBlank() &&
                            shouldFallbackFromMwaToDeeplink(error)
                        ) {
                            Log.w(TAG, "SIWS: MWA failed for $targetPackage, falling back to deeplink", error)
                            loginWithDeeplink(activity, targetPackage)
                        } else {
                            handleMwaError(error)
                        }
                    }
                }
            )
        }
    }

    /**
     * Two-step MWA fallback: authorization already completed, need to generate SIWS
     * challenge in foreground and sign in a second MWA session.
     */
    private suspend fun loginWithMwaTwoStep(
        activity: Activity,
        authResult: MwaAuthResult,
        targetPackage: String?
    ) {
        Log.d(TAG, "SIWS two-step: Generating Privy challenge (foreground)")
        _uiState.value = LoginUiState.Loading

        val siwsMessage = privyAuthManager.generateSiwsMessage(authResult.address).getOrElse { error ->
            Log.e(TAG, "SIWS two-step: Failed to generate message: ${error.message}")
            _uiState.value = LoginUiState.Error(error.message ?: "Failed to prepare sign-in")
            return
        }

        Log.d(TAG, "SIWS two-step: Got challenge (length=${siwsMessage.length}), signing in second session")
        _uiState.value = LoginUiState.MwaSigning

        val signatureBytes = mwaManager.signMessage(
            activity,
            siwsMessage.toByteArray(Charsets.UTF_8),
            targetPackage
        ).getOrElse { error ->
            handleMwaError(error)
            return
        }

        if (targetPackage != null) {
            walletPreferences.setActiveWalletPackage(targetPackage)
        }

        waitForForeground()
        completeSiwsLogin(authResult, siwsMessage, signatureBytes)
    }

    /**
     * Complete the SIWS login flow. Shared by both single-session and two-step paths.
     * Uses the exact SIWS message that was signed (same nonce) for Privy verification.
     */
    private suspend fun completeSiwsLogin(
        authResult: MwaAuthResult,
        siwsMessage: String,
        signatureBytes: ByteArray
    ) {
        _uiState.value = LoginUiState.Loading
        val base64Sig = android.util.Base64.encodeToString(signatureBytes, android.util.Base64.NO_WRAP)
        Log.d(TAG, "SIWS: Completing Privy login for ${authResult.address.take(8)}...")

        privyAuthManager.loginWithSiws(
            message = siwsMessage,
            signature = base64Sig,
            walletClientType = authResult.walletClientType
        ).fold(
            onSuccess = { user ->
                Log.d(TAG, "SIWS: Privy login successful, user=${user.id}")
                // Auth state collector will handle the success event
            },
            onFailure = { error ->
                Log.e(TAG, "SIWS: Privy login failed: ${error.message}")
                _uiState.value = LoginUiState.Error(error.message ?: "Wallet login failed")
            }
        )
    }

    /**
     * Wait for app to return to foreground after MWA session.
     * MWA WebSocket delivers the response before our Activity resumes.
     */
    private suspend fun waitForForeground() {
        val appLifecycle = ProcessLifecycleOwner.get().lifecycle
        var foregroundWait = 0
        while (!appLifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && foregroundWait < 100) {
            delay(50)
            foregroundWait++
        }
        Log.d(TAG, "SIWS: Foreground wait: ${foregroundWait * 50}ms")
    }

    /**
     * Initiate login via wallet deeplink protocol (for wallets without working MWA).
     *
     * This is a 2-step flow:
     * 1. Connect: opens wallet to get wallet address
     * 2. SignMessage: opens wallet again to sign SIWS challenge
     *
     * The wallet redirects back to desperse://wallet-callback after each step.
     * handleWalletCallback() processes each response.
     */
    fun loginWithDeeplink(activity: Activity, walletPackage: String) {
        _uiState.value = LoginUiState.MwaConnecting

        // Persist which wallet app was used so future transactions target it directly
        viewModelScope.launch { walletPreferences.setActiveWalletPackage(walletPackage) }

        val connectIntent = deeplinkWalletManager.startConnect(walletPackage)
        if (connectIntent == null) {
            _uiState.value = LoginUiState.Error("Could not connect to wallet")
            return
        }

        try {
            activity.startActivity(connectIntent)
            Log.d(TAG, "Deeplink: Launched connect intent for $walletPackage")
        } catch (e: Exception) {
            Log.e(TAG, "Deeplink: Failed to launch connect intent", e)
            _uiState.value = LoginUiState.Error("Could not open wallet app")
            deeplinkWalletManager.clearSession()
        }
    }

    /**
     * Handle a wallet deeplink callback (desperse://wallet-callback?...).
     * Called from MainActivity.onNewIntent when the wallet redirects back.
     *
     * Determines whether this is a connect or sign response based on session state,
     * and advances the flow accordingly.
     */
    fun handleWalletCallback(activity: Activity, data: android.net.Uri) {
        Log.d(TAG, "handleWalletCallback: $data")

        // Ignore stale/replayed callbacks when no active deeplink session
        if (!deeplinkWalletManager.hasActiveSession()) {
            Log.d(TAG, "handleWalletCallback: Ignoring stale callback, no active session")
            return
        }

        if (deeplinkWalletManager.isAwaitingSignResponse()) {
            // Step 4: This is a sign response (connect already completed)
            handleSignCallback(data)
        } else {
            // Step 2: This is a connect response
            handleConnectCallback(activity, data)
        }
    }

    private fun handleConnectCallback(activity: Activity, data: android.net.Uri) {
        _uiState.value = LoginUiState.Loading

        val walletAddress = deeplinkWalletManager.handleConnectResponse(data)
        if (walletAddress == null) {
            _uiState.value = LoginUiState.Error("Failed to connect to wallet")
            deeplinkWalletManager.clearSession()
            return
        }

        Log.d(TAG, "Deeplink: Connected, address=${walletAddress.take(8)}...")

        // Step 3: Generate SIWS challenge and open wallet for signing
        viewModelScope.launch {
            val generateResult = privyAuthManager.generateSiwsMessage(walletAddress)
            val message = generateResult.getOrElse { error ->
                Log.e(TAG, "Deeplink: Failed to generate SIWS message", error)
                _uiState.value = LoginUiState.Error(error.message ?: "Failed to prepare sign-in")
                deeplinkWalletManager.clearSession()
                return@launch
            }

            deeplinkWalletManager.pendingSiwsMessage = message
            _uiState.value = LoginUiState.MwaSigning

            val signIntent = deeplinkWalletManager.startSignMessage(message.toByteArray(Charsets.UTF_8))
            if (signIntent == null) {
                _uiState.value = LoginUiState.Error("Failed to prepare sign request")
                deeplinkWalletManager.clearSession()
                return@launch
            }

            try {
                activity.startActivity(signIntent)
                Log.d(TAG, "Deeplink: Launched signMessage intent")
            } catch (e: Exception) {
                Log.e(TAG, "Deeplink: Failed to launch signMessage intent", e)
                _uiState.value = LoginUiState.Error("Could not open wallet app")
                deeplinkWalletManager.clearSession()
            }
        }
    }

    private fun handleSignCallback(data: android.net.Uri) {
        val siwsMessage = deeplinkWalletManager.pendingSiwsMessage
        if (siwsMessage == null) {
            _uiState.value = LoginUiState.Error("Sign-in session expired")
            deeplinkWalletManager.clearSession()
            return
        }

        _uiState.value = LoginUiState.Loading

        val signatureBytes = deeplinkWalletManager.handleSignResponse(data)
        if (signatureBytes == null) {
            _uiState.value = LoginUiState.Error("Wallet rejected the sign request")
            deeplinkWalletManager.clearSession()
            return
        }

        val walletClientType = deeplinkWalletManager.getConnectedWalletClientType()
        val base64Sig = android.util.Base64.encodeToString(signatureBytes, android.util.Base64.NO_WRAP)

        // Complete SIWS login via Privy
        viewModelScope.launch {
            privyAuthManager.loginWithSiws(
                message = siwsMessage,
                signature = base64Sig,
                walletClientType = walletClientType
            ).fold(
                onSuccess = { user ->
                    Log.d(TAG, "Deeplink SIWS: Login successful, user=${user.id}")
                    // Auth state collector will handle the success event
                },
                onFailure = { error ->
                    Log.e(TAG, "Deeplink SIWS: Login failed: ${error.message}")
                    _uiState.value = LoginUiState.Error(error.message ?: "Wallet login failed")
                }
            )
            deeplinkWalletManager.clearSession()
        }
    }

    /** Whether a wallet package should use deeplinks instead of MWA. */
    fun shouldUseDeeplink(packageName: String): Boolean {
        return deeplinkWalletManager.shouldUseDeeplink(packageName)
    }

    /** Solflare experiment: attempt MWA first, fallback to deeplink on transport/runtime failures. */
    fun shouldTryMwaFirst(packageName: String): Boolean {
        return packageName == SOLFLARE_PACKAGE
    }

    private fun shouldFallbackFromMwaToDeeplink(error: Throwable): Boolean {
        return when (error) {
            is MwaError.Timeout,
            is MwaError.SessionTerminated,
            is MwaError.NoWalletInstalled,
            is MwaError.Unknown -> true
            is MwaError.UserCancelled,
            is MwaError.WalletRejected,
            is MwaError.MessageProviderFailed -> false
            else -> true
        }
    }

    /**
     * Handles MWA errors with appropriate UI state transitions.
     */
    private fun handleMwaError(error: Throwable) {
        when (error) {
            is MwaError.UserCancelled -> {
                // User dismissed the wallet app - silently reset to idle
                Log.d(TAG, "MWA operation cancelled by user")
                _uiState.value = LoginUiState.Idle
            }
            is MwaError -> {
                val message = error.userFacingMessage()
                Log.e(TAG, "MWA operation failed: $message", error)
                _uiState.value = LoginUiState.Error(message)
            }
            else -> {
                Log.e(TAG, "MWA operation failed with unexpected error", error)
                _uiState.value = LoginUiState.Error(
                    error.message ?: "Wallet connection failed"
                )
            }
        }
    }

    fun resetToEmailInput() {
        _uiState.value = LoginUiState.Idle
        currentEmail = ""
    }

    fun clearError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }
}
