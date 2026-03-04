package app.desperse.ui.screens.settings

import android.app.Activity
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.arweave.ArweaveUtils
import app.desperse.core.arweave.CreditApproval
import app.desperse.core.arweave.SolTransferBuilder
import app.desperse.core.network.SolanaRpcClient
import app.desperse.core.wallet.TransactionWalletManager
import app.desperse.data.repository.ArweaveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigInteger
import javax.inject.Inject

private const val TAG = "StorageCreditsVM"

data class StorageCreditsUiState(
    val walletAddress: String? = null,
    val walletWinc: String = "0",
    val sharedRemainingWinc: String = "0",
    val hasActiveApproval: Boolean = false,
    val approvals: List<CreditApproval> = emptyList(),
    val fiatRates: Map<String, Double>? = null,
    val rateWinc: String? = null,
    val topUpAmountSol: Double = 0.05,
    val topUpState: TopUpState = TopUpState.Idle,
    val shareState: ShareState = ShareState.Idle,
    val revokeState: RevokeState = RevokeState.Idle,
    val pendingTopUpTx: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val turboSolanaAddress: String? = null
)

sealed class TopUpState {
    data object Idle : TopUpState()
    data object Building : TopUpState()
    data object Signing : TopUpState()
    data object Broadcasting : TopUpState()
    data object Submitting : TopUpState()
    data class Pending(val txId: String) : TopUpState()
    data object Success : TopUpState()
    data class Failed(val error: String) : TopUpState()
}

sealed class ShareState {
    data object Idle : ShareState()
    data object Preparing : ShareState()
    data object Signing : ShareState()
    data object Submitting : ShareState()
    data object Success : ShareState()
    data class Failed(val error: String) : ShareState()
}

sealed class RevokeState {
    data object Idle : RevokeState()
    data object Preparing : RevokeState()
    data object Signing : RevokeState()
    data object Submitting : RevokeState()
    data object Success : RevokeState()
    data class Failed(val error: String) : RevokeState()
}

@HiltViewModel
class StorageCreditsViewModel @Inject constructor(
    private val arweaveRepository: ArweaveRepository,
    private val transactionWalletManager: TransactionWalletManager,
    private val solanaRpcClient: SolanaRpcClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageCreditsUiState())
    val uiState: StateFlow<StorageCreditsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StorageCreditsEvent>()
    val events: SharedFlow<StorageCreditsEvent> = _events.asSharedFlow()

    fun loadData() {
        val walletAddress = transactionWalletManager.getActiveWalletAddress()
        if (walletAddress == null) {
            _uiState.update { it.copy(isLoading = false, error = "No wallet connected") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, walletAddress = walletAddress) }

        viewModelScope.launch {
            // Load service info, balance, and rates in parallel
            val serviceInfoResult = arweaveRepository.getServiceInfo()
            val balanceResult = arweaveRepository.getBalance(walletAddress, forceRefresh = true)
            val ratesResult = arweaveRepository.getFiatRates()

            // Check for pending top-up
            val pendingTx = arweaveRepository.getPendingTopUp()

            serviceInfoResult.onSuccess { info ->
                _uiState.update { it.copy(turboSolanaAddress = info.addresses["solana"]) }
            }

            balanceResult.onSuccess { balance ->
                val sharedRemaining = ArweaveUtils.calculateSharedRemaining(balance.givenApprovals)
                val hasActive = ArweaveUtils.hasActiveApproval(balance.givenApprovals)

                _uiState.update {
                    it.copy(
                        walletWinc = balance.winc,
                        sharedRemainingWinc = sharedRemaining.toString(),
                        hasActiveApproval = hasActive,
                        approvals = balance.givenApprovals
                    )
                }
            }

            ratesResult.onSuccess { rates ->
                _uiState.update {
                    it.copy(fiatRates = rates.fiat, rateWinc = rates.winc)
                }
            }

            val error = when {
                balanceResult.isFailure -> "Failed to load balance: ${balanceResult.exceptionOrNull()?.message}"
                else -> null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = error,
                    pendingTopUpTx = pendingTx
                )
            }

            // Auto-retry pending top-up
            if (pendingTx != null) {
                retryPendingTopUp(pendingTx)
            }
        }
    }

    fun refresh() {
        loadData()
    }

    fun setTopUpAmount(sol: Double) {
        _uiState.update { it.copy(topUpAmountSol = sol) }
    }

    // === Top-Up Flow ===

    fun purchaseCredits(activity: Activity) {
        val state = _uiState.value
        val fromAddress = state.walletAddress ?: return
        val turboAddress = state.turboSolanaAddress ?: run {
            _uiState.update { it.copy(topUpState = TopUpState.Failed("Turbo wallet address not loaded")) }
            return
        }
        val lamports = SolTransferBuilder.solToLamports(state.topUpAmountSol)

        if (_uiState.value.topUpState is TopUpState.Building ||
            _uiState.value.topUpState is TopUpState.Signing ||
            _uiState.value.topUpState is TopUpState.Broadcasting ||
            _uiState.value.topUpState is TopUpState.Submitting
        ) return

        _uiState.update { it.copy(topUpState = TopUpState.Building) }

        viewModelScope.launch {
            try {
                // Step 1: Get recent blockhash
                val blockhashInfo = solanaRpcClient.getLatestBlockhash()
                    .getOrElse { e ->
                        _uiState.update { it.copy(topUpState = TopUpState.Failed("Failed to get blockhash: ${e.message}")) }
                        return@launch
                    }

                // Step 2: Build unsigned transaction
                val unsignedTxBytes = SolTransferBuilder.buildTransferTransaction(
                    fromPubkey = fromAddress,
                    toPubkey = turboAddress,
                    lamports = lamports,
                    recentBlockhash = blockhashInfo.blockhash
                )
                val unsignedTxBase64 = Base64.encodeToString(unsignedTxBytes, Base64.NO_WRAP)

                // Step 3: Sign + broadcast
                _uiState.update { it.copy(topUpState = TopUpState.Signing) }
                val txSignature = transactionWalletManager.signAndSendTransaction(unsignedTxBase64, activity)
                    .getOrElse { e ->
                        _uiState.update { it.copy(topUpState = TopUpState.Failed("Signing failed: ${e.message}")) }
                        return@launch
                    }

                Log.d(TAG, "SOL transfer broadcast, signature: $txSignature")

                // Step 4: Submit to Turbo
                _uiState.update { it.copy(topUpState = TopUpState.Submitting) }
                submitFundTransaction(txSignature)

            } catch (e: Exception) {
                Log.e(TAG, "Top-up failed", e)
                _uiState.update { it.copy(topUpState = TopUpState.Failed(e.message ?: "Unknown error")) }
            }
        }
    }

    private suspend fun submitFundTransaction(txId: String) {
        arweaveRepository.submitFundTransaction(txId)
            .onSuccess { response ->
                when {
                    response.creditedTransaction != null -> {
                        Log.d(TAG, "Credits topped up: ${response.creditedTransaction.winstonCreditAmount} winc")
                        _uiState.update { it.copy(topUpState = TopUpState.Success, pendingTopUpTx = null) }
                        arweaveRepository.invalidateBalanceCache()
                        _events.emit(StorageCreditsEvent.TopUpComplete)
                        // Refresh balance after short delay
                        delay(2000)
                        refresh()
                    }
                    response.pendingTransaction != null -> {
                        Log.d(TAG, "Top-up pending: ${response.pendingTransaction.transactionId}")
                        _uiState.update {
                            it.copy(
                                topUpState = TopUpState.Pending(txId),
                                pendingTopUpTx = txId
                            )
                        }
                        pollPendingTopUp(txId)
                    }
                    response.failedTransaction != null -> {
                        _uiState.update {
                            it.copy(
                                topUpState = TopUpState.Failed("Transaction failed on-chain"),
                                pendingTopUpTx = txId
                            )
                        }
                    }
                }
            }
            .onFailure { e ->
                Log.e(TAG, "Failed to submit fund tx to Turbo", e)
                _uiState.update {
                    it.copy(
                        topUpState = TopUpState.Failed("Failed to register payment: ${e.message}"),
                        pendingTopUpTx = txId
                    )
                }
            }
    }

    private fun pollPendingTopUp(txId: String) {
        viewModelScope.launch {
            val maxPollTime = 60_000L
            val pollInterval = 5_000L
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < maxPollTime) {
                delay(pollInterval)
                arweaveRepository.submitFundTransaction(txId)
                    .onSuccess { response ->
                        when {
                            response.creditedTransaction != null -> {
                                _uiState.update { it.copy(topUpState = TopUpState.Success, pendingTopUpTx = null) }
                                arweaveRepository.invalidateBalanceCache()
                                _events.emit(StorageCreditsEvent.TopUpComplete)
                                delay(2000)
                                refresh()
                                return@launch
                            }
                            response.failedTransaction != null -> {
                                _uiState.update {
                                    it.copy(topUpState = TopUpState.Failed("Transaction failed"), pendingTopUpTx = txId)
                                }
                                return@launch
                            }
                            // pendingTransaction — continue polling
                        }
                    }
                // On error, continue polling
            }
            // Timeout
            _uiState.update {
                it.copy(topUpState = TopUpState.Failed("Payment processing timed out. It may still complete."))
            }
        }
    }

    fun retryPendingTopUp() {
        val txId = _uiState.value.pendingTopUpTx ?: return
        retryPendingTopUp(txId)
    }

    private fun retryPendingTopUp(txId: String) {
        _uiState.update { it.copy(topUpState = TopUpState.Submitting) }
        viewModelScope.launch {
            submitFundTransaction(txId)
        }
    }

    fun dismissTopUpState() {
        _uiState.update { it.copy(topUpState = TopUpState.Idle) }
    }

    // === Authorize (Share Credits) Flow ===

    fun authorizeCredits(activity: Activity, wincAmount: String) {
        if (_uiState.value.shareState !is ShareState.Idle &&
            _uiState.value.shareState !is ShareState.Failed
        ) return

        _uiState.update { it.copy(shareState = ShareState.Preparing) }

        viewModelScope.launch {
            try {
                // Step 1: Prepare — server builds unsigned ANS-104 data item
                val prepareResult = arweaveRepository.prepareShareCredits(wincAmount)
                    .getOrElse { e ->
                        _uiState.update { it.copy(shareState = ShareState.Failed("Prepare failed: ${e.message}")) }
                        return@launch
                    }

                // Step 2: Sign the deep hash
                _uiState.update { it.copy(shareState = ShareState.Signing) }
                val deepHashBytes = Base64.decode(prepareResult.deepHashBase64, Base64.NO_WRAP)
                val signatureBytes = transactionWalletManager.signMessage(deepHashBytes, activity)
                    .getOrElse { e ->
                        _uiState.update { it.copy(shareState = ShareState.Failed("Signing failed: ${e.message}")) }
                        return@launch
                    }
                val signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

                // Step 3: Submit signed data item
                _uiState.update { it.copy(shareState = ShareState.Submitting) }
                arweaveRepository.submitShareCredits(prepareResult.sessionId, signatureBase64)
                    .onSuccess { result ->
                        Log.d(TAG, "Credits shared: ${result.approvedWincAmount} winc")
                        _uiState.update { it.copy(shareState = ShareState.Success) }
                        _events.emit(StorageCreditsEvent.AuthorizationComplete)
                        arweaveRepository.invalidateBalanceCache()
                        delay(2000)
                        refresh()
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(shareState = ShareState.Failed("Submit failed: ${e.message}")) }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Share credits failed", e)
                _uiState.update { it.copy(shareState = ShareState.Failed(e.message ?: "Unknown error")) }
            }
        }
    }

    fun dismissShareState() {
        _uiState.update { it.copy(shareState = ShareState.Idle) }
    }

    // === Revoke Flow ===

    fun revokeCredits(activity: Activity) {
        if (_uiState.value.revokeState !is RevokeState.Idle &&
            _uiState.value.revokeState !is RevokeState.Failed
        ) return

        _uiState.update { it.copy(revokeState = RevokeState.Preparing) }

        viewModelScope.launch {
            try {
                val prepareResult = arweaveRepository.prepareRevokeCredits()
                    .getOrElse { e ->
                        _uiState.update { it.copy(revokeState = RevokeState.Failed("Prepare failed: ${e.message}")) }
                        return@launch
                    }

                _uiState.update { it.copy(revokeState = RevokeState.Signing) }
                val deepHashBytes = Base64.decode(prepareResult.deepHashBase64, Base64.NO_WRAP)
                val signatureBytes = transactionWalletManager.signMessage(deepHashBytes, activity)
                    .getOrElse { e ->
                        _uiState.update { it.copy(revokeState = RevokeState.Failed("Signing failed: ${e.message}")) }
                        return@launch
                    }
                val signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

                _uiState.update { it.copy(revokeState = RevokeState.Submitting) }
                arweaveRepository.submitRevokeCredits(prepareResult.sessionId, signatureBase64)
                    .onSuccess {
                        Log.d(TAG, "Credits revoked")
                        _uiState.update { it.copy(revokeState = RevokeState.Success) }
                        _events.emit(StorageCreditsEvent.RevocationComplete)
                        arweaveRepository.invalidateBalanceCache()
                        delay(2000)
                        refresh()
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(revokeState = RevokeState.Failed("Submit failed: ${e.message}")) }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Revoke credits failed", e)
                _uiState.update { it.copy(revokeState = RevokeState.Failed(e.message ?: "Unknown error")) }
            }
        }
    }

    fun dismissRevokeState() {
        _uiState.update { it.copy(revokeState = RevokeState.Idle) }
    }
}

sealed class StorageCreditsEvent {
    data object TopUpComplete : StorageCreditsEvent()
    data object AuthorizationComplete : StorageCreditsEvent()
    data object RevocationComplete : StorageCreditsEvent()
}
