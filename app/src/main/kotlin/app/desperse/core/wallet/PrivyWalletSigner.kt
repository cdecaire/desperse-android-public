package app.desperse.core.wallet

import android.app.Activity
import android.util.Base64
import android.util.Log
import app.desperse.core.auth.PrivyAuthManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivyWalletSigner @Inject constructor(
    private val privyAuthManager: PrivyAuthManager
) : WalletSigner {
    companion object {
        private const val TAG = "PrivyWalletSigner"
    }

    override suspend fun signTransaction(unsignedTx: ByteArray, activity: Activity): Result<ByteArray> {
        // Privy expects base64 encoded transaction
        val txBase64 = Base64.encodeToString(unsignedTx, Base64.NO_WRAP)
        return privyAuthManager.signTransaction(txBase64).map { signedTxBase64 ->
            Base64.decode(signedTxBase64, Base64.NO_WRAP)
        }
    }

    override suspend fun signMessage(message: ByteArray, activity: Activity): Result<ByteArray> {
        val messageStr = message.toString(Charsets.UTF_8)
        return privyAuthManager.signMessage(messageStr).map { signature ->
            // Privy returns base64 signature
            Base64.decode(signature, Base64.NO_WRAP)
        }
    }
}
