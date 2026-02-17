package app.desperse.ui.screens.settings

import android.app.Activity
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.auth.OAuthProvider
import app.desperse.core.auth.PrivyAuthManager
import app.desperse.core.network.ApiResult
import app.desperse.core.wallet.DeeplinkWalletManager
import app.desperse.core.wallet.MwaManager
import app.desperse.core.wallet.WalletInfo
import app.desperse.core.wallet.WalletPreferences
import app.desperse.core.wallet.WalletType
import app.desperse.data.repository.WalletRepository
import io.privy.auth.LinkedAccount
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.InstalledWallet
import app.desperse.ui.components.WalletPickerSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private const val TAG = "WalletsSettingsVM"

@HiltViewModel
class WalletsSettingsViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val walletPreferences: WalletPreferences,
    private val mwaManager: MwaManager,
    private val privyAuthManager: PrivyAuthManager,
    private val deeplinkWalletManager: DeeplinkWalletManager
) : ViewModel() {

    data class WalletsUiState(
        val wallets: List<WalletInfo> = emptyList(),
        val isLoading: Boolean = true,
        val isMwaAvailable: Boolean = false,
        val linkedAccounts: List<LinkedAccount> = emptyList(),
        val linkingProvider: OAuthProvider? = null,
        val unlinkingSubject: String? = null,
        val walletLinkingInProgress: Boolean = false,
        val walletUnlinkingId: String? = null,
        val walletToUnlink: WalletInfo? = null,
        val showWalletPicker: Boolean = false,
        val availableWallets: List<InstalledWallet> = emptyList()
    )

    private val _uiState = MutableStateFlow(WalletsUiState())
    val uiState: StateFlow<WalletsUiState> = _uiState.asStateFlow()

    sealed class Event {
        data class Success(val message: String) : Event()
        data class Error(val message: String) : Event()
    }

    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<Event>()
    val events = _events

    init {
        loadWallets()
        checkMwaAvailability()
        loadLinkedAccounts()
        resumeDeeplinkIfNeeded()
    }

    fun loadWallets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = walletRepository.getUserWallets()) {
                is ApiResult.Success -> {
                    val walletInfos = result.data.map { it.toWalletInfo() }
                    _uiState.update { it.copy(wallets = walletInfos, isLoading = false) }
                    Log.d(TAG, "Loaded ${walletInfos.size} wallets from server")
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Failed to load wallets: ${result.message}")
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun setDefaultWallet(walletId: String) {
        viewModelScope.launch {
            when (val result = walletRepository.setDefaultWallet(walletId)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Set default wallet: $walletId")
                    loadWallets()
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Failed to set default wallet: ${result.message}")
                }
            }
        }
    }

    fun requestUnlinkWallet(wallet: WalletInfo) {
        if (wallet.type == WalletType.EMBEDDED) {
            viewModelScope.launch { _events.emit(Event.Error("Cannot unlink embedded wallet")) }
            return
        }
        _uiState.update { it.copy(walletToUnlink = wallet) }
    }

    fun dismissUnlinkConfirmation() {
        _uiState.update { it.copy(walletToUnlink = null) }
    }

    fun confirmUnlinkWallet() {
        val wallet = _uiState.value.walletToUnlink ?: return
        _uiState.update { it.copy(walletToUnlink = null, walletUnlinkingId = wallet.id) }

        viewModelScope.launch {
            try {
                // 1) Unlink from Privy
                privyAuthManager.unlinkWallet(wallet.address).getOrThrow()
                Log.d(TAG, "Unlinked ${wallet.address.take(8)}... from Privy")

                // 2) Remove from server DB
                when (val result = walletRepository.removeWallet(wallet.id)) {
                    is ApiResult.Success -> Log.d(TAG, "Removed wallet ${wallet.id} from server")
                    is ApiResult.Error -> Log.w(TAG, "Server remove failed: ${result.message}")
                }

                loadLinkedAccounts()
                loadWallets()
                _events.emit(Event.Success("Wallet unlinked"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unlink wallet", e)
                _events.emit(Event.Error(e.message ?: "Failed to unlink wallet"))
            } finally {
                _uiState.update { it.copy(walletUnlinkingId = null) }
            }
        }
    }

    private fun checkMwaAvailability() {
        // Show link button if any MWA or deeplink-compatible wallet is installed
        val hasMwa = mwaManager.isAvailable()
        _uiState.update { it.copy(isMwaAvailable = hasMwa) }
    }

    private fun loadLinkedAccounts() {
        val accounts = privyAuthManager.getLinkedAccounts()
        _uiState.update { it.copy(linkedAccounts = accounts) }
    }

    fun linkOAuth(provider: OAuthProvider) {
        viewModelScope.launch {
            _uiState.update { it.copy(linkingProvider = provider) }
            privyAuthManager.linkOAuth(provider).fold(
                onSuccess = {
                    loadLinkedAccounts()
                    _events.emit(Event.Success("${provider.id.replaceFirstChar { it.uppercase() }} linked"))
                },
                onFailure = { error ->
                    _events.emit(Event.Error(error.message ?: "Failed to link ${provider.id}"))
                }
            )
            _uiState.update { it.copy(linkingProvider = null) }
        }
    }

    fun unlinkOAuth(provider: OAuthProvider, subject: String) {
        // Check login method count - must keep at least 1
        val socials = _uiState.value.linkedAccounts.count {
            it is LinkedAccount.GoogleOAuthAccount ||
                it is LinkedAccount.TwitterOAuthAccount ||
                it is LinkedAccount.DiscordOAuthAccount
        }
        val externalWallets = _uiState.value.linkedAccounts.count {
            it is LinkedAccount.ExternalWalletAccount
        }
        if (socials + externalWallets <= 1) {
            viewModelScope.launch {
                _events.emit(Event.Error("Cannot unlink your only login method"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(unlinkingSubject = subject) }
            privyAuthManager.unlinkOAuth(provider, subject).fold(
                onSuccess = {
                    loadLinkedAccounts()
                    _events.emit(Event.Success("${provider.id.replaceFirstChar { it.uppercase() }} unlinked"))
                },
                onFailure = { error ->
                    _events.emit(Event.Error(error.message ?: "Failed to unlink ${provider.id}"))
                }
            )
            _uiState.update { it.copy(unlinkingSubject = null) }
        }
    }

    // ========================================================================
    // Wallet Linking
    // ========================================================================

    /**
     * Discover installed wallets and start the link flow.
     * If only one wallet is available, start linking directly.
     * If multiple, show the wallet picker sheet.
     */
    fun onLinkWalletClick(activity: Activity) {
        if (_uiState.value.walletLinkingInProgress) return

        // Gather MWA wallets
        val mwaWallets = mwaManager.getInstalledWallets()

        // Gather deeplink-preferred wallets (Solflare uses deeplinks, not MWA)
        val deeplinkWallets = DeeplinkWalletManager.WALLET_DEEPLINK_URLS.keys
            .filter { pkg -> deeplinkWalletManager.shouldUseDeeplink(pkg) }
            .filter { pkg -> mwaWallets.none { it.packageName == pkg } }
            .mapNotNull { pkg ->
                val name = when (pkg) {
                    "com.solflare.mobile" -> "Solflare"
                    else -> return@mapNotNull null
                }
                InstalledWallet(
                    packageName = pkg,
                    displayName = name,
                    walletClientType = when (pkg) {
                        "com.solflare.mobile" -> "solflare"
                        else -> "unknown"
                    }
                )
            }

        val allWallets = mwaWallets.map {
            InstalledWallet(it.packageName, it.displayName, it.walletClientType)
        } + deeplinkWallets

        if (allWallets.isEmpty()) {
            viewModelScope.launch {
                _events.emit(Event.Error("No compatible wallet apps found"))
            }
            return
        }

        if (allWallets.size == 1) {
            onWalletSelected(allWallets.first(), activity)
        } else {
            _uiState.update { it.copy(showWalletPicker = true, availableWallets = allWallets) }
        }
    }

    fun dismissWalletPicker() {
        _uiState.update { it.copy(showWalletPicker = false) }
    }

    fun onWalletSelected(wallet: InstalledWallet, activity: Activity) {
        _uiState.update { it.copy(showWalletPicker = false) }

        if (deeplinkWalletManager.shouldUseDeeplink(wallet.packageName)) {
            linkViaDeeplink(wallet, activity)
        } else {
            linkViaMwa(wallet, activity)
        }
    }

    /**
     * Link via MWA: authorize + sign SIWS message in a single wallet session.
     */
    private fun linkViaMwa(wallet: InstalledWallet, activity: Activity) {
        _uiState.update { it.copy(walletLinkingInProgress = true) }

        viewModelScope.launch {
            try {
                // Store the SIWS message string from the messageProvider so we can reuse it
                // (calling generateSiwsMessage twice would create different challenges)
                var siwsMessage: String? = null

                val result = mwaManager.authorizeAndSignMessage(
                    activity = activity,
                    targetPackage = wallet.packageName
                ) { address ->
                    val messageResult = privyAuthManager.generateSiwsMessage(address)
                    val message = messageResult.getOrThrow()
                    siwsMessage = message
                    message.toByteArray(Charsets.UTF_8)
                }

                result.fold(
                    onSuccess = { (authResult, signatureBytes) ->
                        val signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
                        val message = siwsMessage
                            ?: throw IllegalStateException("SIWS message was not captured")

                        privyAuthManager.linkWithSiws(
                            walletAddress = authResult.address,
                            message = message,
                            signatureBase64 = signatureBase64,
                            walletClientType = authResult.walletClientType
                        ).getOrThrow()

                        // Register wallet in backend DB
                        val label = authResult.walletLabel ?: "${wallet.displayName} Wallet"
                        val dbError = walletRepository.ensureWalletExists(
                            authResult.address, "external", "mwa", label
                        )

                        loadLinkedAccounts()
                        loadWallets()
                        if (dbError == null) {
                            _events.emit(Event.Success("Wallet linked successfully"))
                        } else {
                            _events.emit(Event.Error("Linked in Privy but server failed: $dbError"))
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "MWA wallet linking failed", error)
                        _events.emit(Event.Error(error.message ?: "Failed to link wallet"))
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "MWA wallet linking exception", e)
                _events.emit(Event.Error(e.message ?: "Failed to link wallet"))
            } finally {
                _uiState.update { it.copy(walletLinkingInProgress = false) }
            }
        }
    }

    /**
     * Check if there's a deeplink flow that was interrupted by process death.
     * If so, try to resume it by collecting the callback from the replay buffer.
     */
    private fun resumeDeeplinkIfNeeded() {
        val state = deeplinkWalletManager.flowState
        if (state == DeeplinkWalletManager.FlowState.IDLE) return

        Log.d(TAG, "Found pending deeplink flow: state=$state")
        _uiState.update { it.copy(walletLinkingInProgress = true) }

        viewModelScope.launch {
            try {
                val callbackFlow = deeplinkWalletManager.walletCallbacks

                when (state) {
                    DeeplinkWalletManager.FlowState.AWAITING_CONNECT -> {
                        // Process was killed after connect deeplink was opened.
                        // Check if the connect callback is in the replay buffer.
                        val connectUri = withTimeoutOrNull(3000L) { callbackFlow.first() }
                        if (connectUri != null) {
                            Log.d(TAG, "Resuming deeplink: got connect callback after process restart")
                            val address = deeplinkWalletManager.handleConnectResponse(connectUri)
                            if (address != null) {
                                // Connected! Now need to generate SIWS and open sign deeplink.
                                // But we don't have an Activity reference here (init).
                                // Store the state and let the user click "Continue" or auto-trigger.
                                Log.d(TAG, "Resumed connect: $address — but need Activity to continue sign step")
                                _events.emit(Event.Error("Wallet connected but signing was interrupted. Please try linking again."))
                            } else {
                                val reason = deeplinkWalletManager.lastError ?: "Unknown error"
                                _events.emit(Event.Error("Connection failed: $reason"))
                            }
                        } else {
                            Log.d(TAG, "No connect callback available — flow expired")
                            _events.emit(Event.Error("Wallet connection was interrupted. Please try again."))
                        }
                        deeplinkWalletManager.clearSession()
                    }

                    DeeplinkWalletManager.FlowState.AWAITING_SIGN -> {
                        // Process was killed after sign deeplink was opened.
                        // Check if the sign callback is in the replay buffer.
                        val signUri = withTimeoutOrNull(3000L) { callbackFlow.first() }
                        if (signUri != null) {
                            Log.d(TAG, "Resuming deeplink: got sign callback after process restart")
                            val signatureBytes = deeplinkWalletManager.handleSignResponse(signUri)
                            val message = deeplinkWalletManager.pendingSiwsMessage
                            val address = deeplinkWalletManager.getConnectedAddress()

                            if (signatureBytes != null && message != null && address != null) {
                                // We have everything needed to complete the link!
                                val signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
                                val walletClientType = deeplinkWalletManager.getConnectedWalletClientType()

                                // Re-generate SIWS params (needed by Privy)
                                privyAuthManager.generateSiwsMessage(address)

                                Log.d(TAG, "Resuming: linking wallet via Privy SIWS...")
                                privyAuthManager.linkWithSiws(
                                    walletAddress = address,
                                    message = message,
                                    signatureBase64 = signatureBase64,
                                    walletClientType = walletClientType
                                ).getOrThrow()

                                val label = when (deeplinkWalletManager.getConnectedWalletClientType()) {
                                    "solflare" -> "Solflare Wallet"
                                    "phantom" -> "Phantom Wallet"
                                    else -> "External Wallet"
                                }
                                val dbError = walletRepository.ensureWalletExists(
                                    address, "external", "deeplink", label
                                )

                                loadLinkedAccounts()
                                loadWallets()
                                if (dbError == null) {
                                    _events.emit(Event.Success("Wallet linked successfully"))
                                } else {
                                    _events.emit(Event.Error("Linked in Privy but server failed: $dbError"))
                                }
                            } else {
                                val reason = deeplinkWalletManager.lastError ?: "Missing data after process restart"
                                _events.emit(Event.Error("Signing failed: $reason"))
                            }
                        } else {
                            Log.d(TAG, "No sign callback available — flow expired")
                            _events.emit(Event.Error("Wallet signing was interrupted. Please try again."))
                        }
                        deeplinkWalletManager.clearSession()
                    }

                    DeeplinkWalletManager.FlowState.IDLE -> { /* Already checked above */ }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume deeplink flow", e)
                _events.emit(Event.Error(e.message ?: "Failed to resume wallet linking"))
                deeplinkWalletManager.clearSession()
            } finally {
                _uiState.update { it.copy(walletLinkingInProgress = false) }
            }
        }
    }

    /**
     * Link via deeplink protocol (Phantom/Solflare universal links).
     * Multi-step: connect → get address → generate SIWS → sign → link.
     */
    private fun linkViaDeeplink(wallet: InstalledWallet, activity: Activity) {
        _uiState.update { it.copy(walletLinkingInProgress = true) }

        viewModelScope.launch {
            try {
                // Clear any stale replayed values before starting
                deeplinkWalletManager.resetCallbacks()
                val callbackFlow = deeplinkWalletManager.walletCallbacks

                // Step 1: Connect to wallet
                val connectIntent = deeplinkWalletManager.startConnect(wallet.packageName)
                if (connectIntent == null) {
                    _events.emit(Event.Error("Cannot connect to ${wallet.displayName}"))
                    _uiState.update { it.copy(walletLinkingInProgress = false) }
                    return@launch
                }

                Log.d(TAG, "Launching ${wallet.displayName} connect deeplink")
                activity.startActivity(connectIntent)

                // Wait for connect callback from wallet
                Log.d(TAG, "Waiting for connect callback...")
                val connectUri = callbackFlow.first()
                Log.d(TAG, "Got connect callback: $connectUri")

                val address = deeplinkWalletManager.handleConnectResponse(connectUri)
                if (address == null) {
                    val reason = deeplinkWalletManager.lastError ?: "Unknown error"
                    _events.emit(Event.Error("Connection failed: $reason"))
                    deeplinkWalletManager.clearSession()
                    _uiState.update { it.copy(walletLinkingInProgress = false) }
                    return@launch
                }

                Log.d(TAG, "Connected to ${address.take(8)}..., generating SIWS message")

                // Step 2: Generate SIWS message
                val messageResult = privyAuthManager.generateSiwsMessage(address)
                val message = messageResult.getOrElse { error ->
                    _events.emit(Event.Error("Failed to generate challenge: ${error.message}"))
                    deeplinkWalletManager.clearSession()
                    _uiState.update { it.copy(walletLinkingInProgress = false) }
                    return@launch
                }

                deeplinkWalletManager.pendingSiwsMessage = message

                // Step 3: Sign message
                val signIntent = deeplinkWalletManager.startSignMessage(message.toByteArray(Charsets.UTF_8))
                if (signIntent == null) {
                    _events.emit(Event.Error("Failed to prepare signing request"))
                    deeplinkWalletManager.clearSession()
                    _uiState.update { it.copy(walletLinkingInProgress = false) }
                    return@launch
                }

                // Reset replay buffer again before the sign callback
                deeplinkWalletManager.resetCallbacks()

                Log.d(TAG, "Launching ${wallet.displayName} sign deeplink")
                activity.startActivity(signIntent)

                Log.d(TAG, "Waiting for sign callback...")
                val signUri = callbackFlow.first()
                Log.d(TAG, "Got sign callback: $signUri")

                val signatureBytes = deeplinkWalletManager.handleSignResponse(signUri)
                if (signatureBytes == null) {
                    val reason = deeplinkWalletManager.lastError ?: "Unknown error"
                    _events.emit(Event.Error("Signing failed: $reason"))
                    deeplinkWalletManager.clearSession()
                    _uiState.update { it.copy(walletLinkingInProgress = false) }
                    return@launch
                }

                // Step 4: Link via Privy (Base64 signature)
                val signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

                Log.d(TAG, "Linking wallet via Privy SIWS...")
                privyAuthManager.linkWithSiws(
                    walletAddress = address,
                    message = message,
                    signatureBase64 = signatureBase64,
                    walletClientType = deeplinkWalletManager.getConnectedWalletClientType()
                ).getOrThrow()

                val label = "${wallet.displayName} Wallet"
                val dbError = walletRepository.ensureWalletExists(
                    address, "external", "deeplink", label
                )
                Log.d(TAG, "ensureWalletExists error: $dbError")

                deeplinkWalletManager.clearSession()

                loadLinkedAccounts()
                loadWallets()
                if (dbError == null) {
                    _events.emit(Event.Success("Wallet linked successfully"))
                } else {
                    _events.emit(Event.Error("Linked in Privy but server failed: $dbError"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Deeplink wallet linking exception", e)
                deeplinkWalletManager.clearSession()
                _events.emit(Event.Error(e.message ?: "Failed to link wallet"))
            } finally {
                _uiState.update { it.copy(walletLinkingInProgress = false) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsSettingsScreen(
    onBack: () -> Unit,
    viewModel: WalletsSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WalletsSettingsViewModel.Event.Success ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is WalletsSettingsViewModel.Event.Error ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val linkedSocials = remember(uiState.linkedAccounts) {
        uiState.linkedAccounts.filter {
            it is LinkedAccount.GoogleOAuthAccount || it is LinkedAccount.TwitterOAuthAccount
        }
    }
    val hasGoogle = linkedSocials.any { it is LinkedAccount.GoogleOAuthAccount }
    val hasTwitter = linkedSocials.any { it is LinkedAccount.TwitterOAuthAccount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallets & Linked") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        FaIcon(FaIcons.ArrowLeft, size = 20.dp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Manage your connected wallets and linked accounts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // -- Wallets section --
            Text(
                text = "Wallets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Loading wallets...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (uiState.wallets.isNotEmpty()) {
                val showSelection = uiState.wallets.size > 1
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    uiState.wallets.forEach { wallet ->
                        WalletRow(
                            wallet = wallet,
                            showSelection = showSelection,
                            isUnlinking = uiState.walletUnlinkingId == wallet.id,
                            onClick = if (showSelection && !wallet.isPrimary) {
                                { viewModel.setDefaultWallet(wallet.id) }
                            } else null,
                            onUnlink = if (wallet.type == WalletType.EXTERNAL) {
                                { viewModel.requestUnlinkWallet(wallet) }
                            } else null
                        )
                    }
                }
            } else {
                Text(
                    text = "No wallets found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            if (uiState.isMwaAvailable) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.onLinkWalletClick(activity) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.walletLinkingInProgress,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    if (uiState.walletLinkingInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        FaIcon(FaIcons.Plus, size = 14.dp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.walletLinkingInProgress) "Linking..." else "Link wallet")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -- Linked Social Accounts section --
            Text(
                text = "Linked social accounts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Link social accounts for easier login and account recovery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Link buttons for unlinked providers
            if (!hasGoogle || !hasTwitter) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hasGoogle) {
                        OutlinedButton(
                            onClick = { viewModel.linkOAuth(OAuthProvider.GOOGLE) },
                            enabled = uiState.linkingProvider == null,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            if (uiState.linkingProvider == OAuthProvider.GOOGLE) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                FaIcon(FaIcons.Google, size = 14.dp, style = FaIconStyle.Brands)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Link Google")
                        }
                    }
                    if (!hasTwitter) {
                        OutlinedButton(
                            onClick = { viewModel.linkOAuth(OAuthProvider.TWITTER) },
                            enabled = uiState.linkingProvider == null,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            if (uiState.linkingProvider == OAuthProvider.TWITTER) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                FaIcon(FaIcons.X, size = 14.dp, style = FaIconStyle.Brands)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Link Twitter")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Linked accounts list
            if (linkedSocials.isEmpty()) {
                Text(
                    text = "No linked accounts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    linkedSocials.forEach { account ->
                        LinkedAccountRow(
                            account = account,
                            isUnlinking = when (account) {
                                is LinkedAccount.GoogleOAuthAccount -> uiState.unlinkingSubject == account.subject
                                is LinkedAccount.TwitterOAuthAccount -> uiState.unlinkingSubject == account.subject
                                else -> false
                            },
                            onUnlink = {
                                when (account) {
                                    is LinkedAccount.GoogleOAuthAccount ->
                                        viewModel.unlinkOAuth(OAuthProvider.GOOGLE, account.subject)
                                    is LinkedAccount.TwitterOAuthAccount ->
                                        viewModel.unlinkOAuth(OAuthProvider.TWITTER, account.subject)
                                    else -> {}
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Wallet picker bottom sheet
    if (uiState.showWalletPicker) {
        WalletPickerSheet(
            wallets = uiState.availableWallets,
            onWalletSelected = { wallet -> viewModel.onWalletSelected(wallet, activity) },
            onDismiss = { viewModel.dismissWalletPicker() }
        )
    }

    // Unlink confirmation dialog
    uiState.walletToUnlink?.let { wallet ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUnlinkConfirmation() },
            title = { Text("Unlink wallet?") },
            text = {
                Text("Remove ${wallet.label} (${wallet.address.take(6)}...${wallet.address.takeLast(4)}) from your account? You can re-link it later.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmUnlinkWallet() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Unlink")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUnlinkConfirmation() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * A single wallet card styled as a selectable radio option.
 * Selected wallet gets a highlighted border and filled check circle.
 * Long-press shows a context menu with copy address / export key options.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WalletRow(
    wallet: WalletInfo,
    showSelection: Boolean = true,
    isUnlinking: Boolean = false,
    onClick: (() -> Unit)? = null,
    onUnlink: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isSelected = wallet.isPrimary && showSelection
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }

    var showMenu by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick?.invoke() },
                    onLongClick = { showMenu = true }
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wallet icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    FaIcon(
                        icon = if (wallet.type == WalletType.EXTERNAL) FaIcons.LinkSimple else FaIcons.Wallet,
                        size = 16.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Label + type badge + address
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = wallet.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        // Type badge inline with name
                        val badgeText = when {
                            wallet.type == WalletType.EMBEDDED -> "Embedded"
                            wallet.connector == "deeplink" -> "Deeplink"
                            else -> "MWA"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "${wallet.address.take(6)}...${wallet.address.takeLast(6)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Radio-style selection indicator (only when multiple wallets)
                if (showSelection) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier.background(MaterialTheme.colorScheme.primary)
                                } else {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            FaIcon(
                                icon = FaIcons.Check,
                                size = 12.dp,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        // Long-press context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Copy wallet address") },
                leadingIcon = {
                    FaIcon(FaIcons.Copy, size = 14.dp, tint = MaterialTheme.colorScheme.onSurface)
                },
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Wallet Address", wallet.address))
                    Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                    showMenu = false
                }
            )
            if (wallet.type == WalletType.EMBEDDED) {
                DropdownMenuItem(
                    text = { Text("Export private key") },
                    leadingIcon = {
                        FaIcon(FaIcons.ArrowUpRightSimple, size = 14.dp, tint = MaterialTheme.colorScheme.onSurface)
                    },
                    onClick = {
                        Toast.makeText(context, "Export via desperse.com on web", Toast.LENGTH_SHORT).show()
                        showMenu = false
                    }
                )
            }
            if (wallet.type == WalletType.EXTERNAL && onUnlink != null) {
                DropdownMenuItem(
                    text = { Text("Unlink wallet", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        FaIcon(FaIcons.LinkSlash, size = 14.dp, tint = MaterialTheme.colorScheme.error)
                    },
                    enabled = !isUnlinking,
                    onClick = {
                        showMenu = false
                        onUnlink()
                    }
                )
            }
        }
    }
}

/**
 * A row displaying a linked social account (Google, Twitter).
 */
@Composable
private fun LinkedAccountRow(
    account: LinkedAccount,
    isUnlinking: Boolean,
    onUnlink: () -> Unit
) {
    val (icon, label, detail) = when (account) {
        is LinkedAccount.GoogleOAuthAccount -> Triple(
            FaIcons.Google,
            "Google",
            account.email
        )
        is LinkedAccount.TwitterOAuthAccount -> Triple(
            FaIcons.X,
            "Twitter",
            "@${account.username}"
        )
        else -> return
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                FaIcon(
                    icon = icon,
                    size = 16.dp,
                    style = FaIconStyle.Brands,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name + detail
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Unlink button
            IconButton(
                onClick = onUnlink,
                enabled = !isUnlinking,
                modifier = Modifier.size(32.dp)
            ) {
                if (isUnlinking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    FaIcon(
                        icon = FaIcons.Xmark,
                        size = 14.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Connected badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Connected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
