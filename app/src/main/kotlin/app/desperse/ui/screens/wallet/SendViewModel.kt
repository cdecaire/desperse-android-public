package app.desperse.ui.screens.wallet

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.network.BlockhashExpiredException
import app.desperse.core.network.InsufficientFundsException
import app.desperse.core.wallet.MwaError
import app.desperse.core.wallet.InstalledMwaWallet
import app.desperse.core.wallet.TransactionWalletManager
import app.desperse.data.repository.SendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SendViewModel"

sealed class SendState {
    data object Idle : SendState()
    data object Preparing : SendState()
    data object Signing : SendState()
    data object Submitting : SendState()
    data class Submitted(val signature: String) : SendState()
    data class Failed(val error: String, val canRetry: Boolean = true) : SendState()
}

data class SendUiState(
    val sendState: SendState = SendState.Idle,
    val showWalletPicker: Boolean = false,
    val installedWallets: List<InstalledMwaWallet> = emptyList()
)

@HiltViewModel
class SendViewModel @Inject constructor(
    private val sendRepository: SendRepository,
    private val transactionWalletManager: TransactionWalletManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()

    // Pending send params for wallet picker resume
    private var pendingToAddress: String = ""
    private var pendingAmount: String = ""
    private var pendingAsset: String = ""
    private var pendingActivity: Activity? = null

    /** Called after a successful send to refresh wallet data externally */
    var onSendSuccess: (() -> Unit)? = null

    fun send(
        toAddress: String,
        amount: String,
        asset: String,
        activity: Activity
    ) {
        val currentState = _uiState.value.sendState
        if (currentState is SendState.Preparing ||
            currentState is SendState.Signing ||
            currentState is SendState.Submitting ||
            currentState is SendState.Submitted
        ) return

        // Check wallet availability
        if (!transactionWalletManager.isActiveWalletAvailable()) {
            _uiState.update {
                it.copy(sendState = SendState.Failed(
                    "No compatible wallet app found. Please install a Solana wallet.",
                    canRetry = false
                ))
            }
            return
        }

        // If external wallet package is unknown, show wallet picker
        if (transactionWalletManager.needsWalletSelection()) {
            pendingToAddress = toAddress
            pendingAmount = amount
            pendingAsset = asset
            pendingActivity = activity
            val wallets = transactionWalletManager.getInstalledExternalWallets()
            _uiState.update { it.copy(showWalletPicker = true, installedWallets = wallets) }
            return
        }

        val walletAddress = transactionWalletManager.getActiveWalletAddress()

        viewModelScope.launch {
            // Step 1: Prepare (get unsigned transaction from server)
            _uiState.update { it.copy(sendState = SendState.Preparing) }
            Log.d(TAG, "Send Step 1: Preparing transfer $amount $asset to ${toAddress.take(8)}...")

            sendRepository.prepareSend(toAddress, amount, asset, walletAddress ?: "")
                .onSuccess { prepareResult ->
                    Log.d(TAG, "Got unsigned send tx")

                    // Step 2: Sign and broadcast
                    _uiState.update { it.copy(sendState = SendState.Signing) }
                    Log.d(TAG, "Send Step 2: Signing via active wallet")

                    transactionWalletManager.signAndSendTransaction(
                        prepareResult.transactionBase64, activity
                    )
                        .onSuccess { txSignature ->
                            Log.d(TAG, "Send tx broadcast, signature=$txSignature")
                            _uiState.update { it.copy(sendState = SendState.Submitted(txSignature)) }

                            // Notify caller to refresh wallet data
                            onSendSuccess?.invoke()
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Send sign+broadcast failed: ${error.message}")
                            val errorMessage = mapSigningError(error)
                            _uiState.update {
                                it.copy(sendState = SendState.Failed(
                                    errorMessage,
                                    canRetry = error !is MwaError.NoWalletInstalled
                                ))
                            }
                        }
                }
                .onFailure { error ->
                    Log.e(TAG, "Send prepare failed: ${error.message}")
                    val errorMessage = mapPrepareError(error.message ?: "Failed to prepare transaction")
                    _uiState.update {
                        it.copy(sendState = SendState.Failed(errorMessage))
                    }
                }
        }
    }

    fun onWalletSelectedForSend(packageName: String) {
        val toAddress = pendingToAddress
        val amount = pendingAmount
        val asset = pendingAsset
        val activity = pendingActivity

        viewModelScope.launch {
            transactionWalletManager.setWalletPackage(packageName)
            _uiState.update { it.copy(showWalletPicker = false) }
            if (activity != null && toAddress.isNotBlank()) {
                send(toAddress, amount, asset, activity)
            }
        }
    }

    fun dismissWalletPicker() {
        _uiState.update { it.copy(showWalletPicker = false) }
        pendingActivity = null
        pendingToAddress = ""
        pendingAmount = ""
        pendingAsset = ""
    }

    fun resetState() {
        _uiState.update { it.copy(sendState = SendState.Idle) }
    }

    private fun mapSigningError(error: Throwable): String = when (error) {
        is BlockhashExpiredException -> "Transaction expired. Please try again."
        is InsufficientFundsException -> "Insufficient funds for this transaction."
        is MwaError.UserCancelled -> "Transaction cancelled."
        is MwaError.NoWalletInstalled -> "No compatible wallet app found."
        is MwaError.Timeout -> "Wallet connection timed out. Please try again."
        is MwaError.WalletRejected -> "Wallet rejected the transaction."
        is MwaError.SessionTerminated -> "Wallet session ended. Please try again."
        else -> error.message ?: "Failed to sign transaction"
    }

    private fun mapPrepareError(message: String): String = when {
        message.contains("INSUFFICIENT_SOL_FOR_FEES", ignoreCase = true) ||
            (message.contains("insufficient", ignoreCase = true) && message.contains("fee", ignoreCase = true)) ->
            "Not enough SOL for network fees"
        message.contains("SELF_SEND", ignoreCase = true) ||
            message.contains("Cannot send to yourself", ignoreCase = true) ->
            "Cannot send to your own address"
        message.contains("INVALID_ADDRESS", ignoreCase = true) ||
            message.contains("Invalid", ignoreCase = true) && message.contains("address", ignoreCase = true) ->
            "Invalid recipient address"
        message.contains("INSUFFICIENT_BALANCE", ignoreCase = true) ->
            "Insufficient balance"
        else -> message
    }
}
