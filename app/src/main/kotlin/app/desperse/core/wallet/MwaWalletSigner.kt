package app.desperse.core.wallet

import android.app.Activity
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MwaWalletSigner @Inject constructor(
    private val mwaManager: MwaManager
) : WalletSigner {
    companion object {
        private const val TAG = "MwaWalletSigner"
    }

    override suspend fun signTransaction(unsignedTx: ByteArray, activity: Activity): Result<ByteArray> {
        Log.d(TAG, "Signing transaction via MWA (${unsignedTx.size} bytes)")
        return mwaManager.signTransaction(activity, unsignedTx)
    }

    override suspend fun signMessage(message: ByteArray, activity: Activity): Result<ByteArray> {
        Log.d(TAG, "Signing message via MWA (${message.size} bytes)")
        return mwaManager.signMessage(activity, message)
    }
}
