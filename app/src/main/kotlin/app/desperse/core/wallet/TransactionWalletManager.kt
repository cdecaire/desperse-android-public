package app.desperse.core.wallet

import android.app.Activity
import android.util.Base64
import android.util.Log
import app.desperse.core.auth.PrivyAuthManager
import app.desperse.core.network.SolanaRpcClient
import app.desperse.data.repository.WalletRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes transaction signing to the correct wallet provider based on user preference.
 *
 * Both paths follow the same pattern: sign → broadcast via SolanaRpcClient → return tx signature.
 * - Embedded wallets: Privy signs the transaction
 * - External wallets (MWA): Wallet app signs the transaction (sign-only, no broadcast)
 *
 * Callers get a uniform Result<String> (base58 tx signature) regardless of wallet type.
 */
@Singleton
class TransactionWalletManager @Inject constructor(
    private val privyAuthManager: PrivyAuthManager,
    private val mwaManager: MwaManager,
    private val walletPreferences: WalletPreferences,
    private val solanaRpcClient: SolanaRpcClient,
    private val transactionVerifier: TransactionVerifier,
    private val walletRepository: WalletRepository
) {
    companion object {
        private const val TAG = "TransactionWalletMgr"
    }

    /**
     * Sign and broadcast a transaction using the user's active wallet.
     * Returns the base58 transaction signature on success.
     *
     * - Embedded wallet: Privy signs → broadcast via RPC → return signature
     * - External wallet (MWA): Wallet signs → broadcast via RPC → return signature
     *
     * @param unsignedTxBase64 The unsigned transaction from the server, base64 encoded
     * @param activity Required for MWA wallet interaction (launches wallet app)
     */
    suspend fun signAndSendTransaction(
        unsignedTxBase64: String,
        activity: Activity
    ): Result<String> {
        // Verify transaction structure before signing
        transactionVerifier.verify(unsignedTxBase64).onFailure { error ->
            Log.e(TAG, "Transaction verification failed: ${error.message}")
            return Result.failure(error)
        }

        return when (walletPreferences.getActiveWalletType()) {
            WalletType.EMBEDDED -> signAndBroadcastEmbedded(unsignedTxBase64)
            WalletType.EXTERNAL -> signAndBroadcastMwa(unsignedTxBase64, activity)
        }
    }

    /**
     * Embedded wallet path: sign with Privy, then broadcast via SolanaRpcClient.
     */
    private suspend fun signAndBroadcastEmbedded(unsignedTxBase64: String): Result<String> {
        Log.d(TAG, "Signing via Privy embedded wallet")

        val signedTxBase64 = privyAuthManager.signTransaction(unsignedTxBase64)
            .getOrElse { return Result.failure(it) }

        Log.d(TAG, "Transaction signed, broadcasting via RPC")

        return solanaRpcClient.sendTransaction(signedTxBase64)
    }

    /**
     * MWA wallet path: wallet signs, then we broadcast via SolanaRpcClient.
     * Uses signTransactions (sign-only) instead of the deprecated signAndSendTransactions
     * which some wallets don't handle reliably.
     */
    private suspend fun signAndBroadcastMwa(
        unsignedTxBase64: String,
        activity: Activity
    ): Result<String> {
        val targetPackage = walletPreferences.getActiveWalletPackage()
        Log.d(TAG, "Signing via MWA external wallet (targetPackage=$targetPackage)")

        val txBytes = Base64.decode(unsignedTxBase64, Base64.NO_WRAP)
        val signedTxBytes = mwaManager.signTransaction(activity, txBytes, targetPackage)
            .getOrElse { return Result.failure(it) }

        val signedTxBase64 = Base64.encodeToString(signedTxBytes, Base64.NO_WRAP)
        Log.d(TAG, "MWA transaction signed, broadcasting via RPC")

        return solanaRpcClient.sendTransaction(signedTxBase64)
    }

    /**
     * Sign an arbitrary message using the active wallet.
     */
    suspend fun signMessage(
        message: ByteArray,
        activity: Activity
    ): Result<ByteArray> {
        return when (walletPreferences.getActiveWalletType()) {
            WalletType.EMBEDDED -> {
                val messageStr = message.toString(Charsets.UTF_8)
                privyAuthManager.signMessage(messageStr).map { sig ->
                    Base64.decode(sig, Base64.NO_WRAP)
                }
            }
            WalletType.EXTERNAL -> {
                val targetPackage = walletPreferences.getActiveWalletPackage()
                mwaManager.signMessage(activity, message, targetPackage)
            }
        }
    }

    /**
     * Get the address of the currently active wallet.
     */
    fun getActiveWalletAddress(): String? = walletPreferences.activeWallet.value?.address

    /**
     * Check if the active wallet is reachable on this device.
     * For MWA wallets, this checks if a compatible wallet app is installed.
     */
    fun isActiveWalletAvailable(): Boolean {
        return when (walletPreferences.getActiveWalletType()) {
            WalletType.EMBEDDED -> true // Always available
            WalletType.EXTERNAL -> mwaManager.isAvailable()
        }
    }

    /**
     * Check if we need the user to pick a wallet before signing.
     * Returns true if the active wallet is external but we don't know which app to target,
     * which would cause the Android system wallet picker to appear.
     */
    fun needsWalletSelection(): Boolean {
        return walletPreferences.getActiveWalletType() == WalletType.EXTERNAL
            && walletPreferences.getActiveWalletPackage() == null
    }

    /**
     * Get the list of installed MWA wallet apps for the custom wallet picker.
     */
    fun getInstalledExternalWallets(): List<InstalledMwaWallet> = mwaManager.getInstalledWallets()

    /**
     * Store the selected wallet package for future transaction signing.
     * Also updates the wallet label both locally and on the server.
     */
    suspend fun setWalletPackage(packageName: String) {
        walletPreferences.setActiveWalletPackage(packageName)
        // Sync label to server (best-effort)
        val wallet = walletPreferences.activeWallet.value
        if (wallet != null) {
            walletRepository.updateWalletLabel(wallet.address, wallet.label)
        }
    }
}
