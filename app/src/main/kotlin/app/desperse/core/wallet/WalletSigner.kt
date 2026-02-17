package app.desperse.core.wallet

import android.app.Activity

/**
 * Abstraction over wallet signing operations.
 * Implementations route to either Privy embedded wallet or MWA external wallet.
 * Enables unit testing by mocking this interface.
 */
interface WalletSigner {
    /**
     * Sign a serialized Solana transaction.
     * @param unsignedTx Raw unsigned transaction bytes
     * @param activity Required for MWA (launches wallet app via Intent)
     * @return Signed transaction bytes (for embedded) or tx signature bytes (for MWA signAndSend)
     */
    suspend fun signTransaction(unsignedTx: ByteArray, activity: Activity): Result<ByteArray>

    /**
     * Sign an arbitrary message (e.g., SIWS message for authentication).
     * @param message Message bytes to sign
     * @param activity Required for MWA
     * @return Signature bytes
     */
    suspend fun signMessage(message: ByteArray, activity: Activity): Result<ByteArray>
}
