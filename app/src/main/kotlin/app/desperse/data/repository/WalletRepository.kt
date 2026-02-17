package app.desperse.data.repository

import android.util.Log
import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.ErrorCode
import app.desperse.core.network.safeApiCall
import app.desperse.core.wallet.AddWalletResponse
import app.desperse.core.wallet.UserWalletDto
import app.desperse.core.wallet.WalletPreferences
import app.desperse.data.dto.AddWalletRequest
import app.desperse.data.dto.UpdateWalletLabelRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(
    private val api: DesperseApi,
    private val walletPreferences: WalletPreferences
) {
    companion object {
        private const val TAG = "WalletRepository"
    }

    suspend fun getUserWallets(): ApiResult<List<UserWalletDto>> {
        return when (val result = safeApiCall { api.getUserWallets() }) {
            is ApiResult.Success -> {
                val wallets = result.data.wallets
                walletPreferences.updateWallets(wallets.map { it.toWalletInfo() })
                ApiResult.Success(wallets, result.requestId)
            }
            is ApiResult.Error -> result
        }
    }

    suspend fun addWallet(address: String, type: String, connector: String?, label: String?): ApiResult<AddWalletResponse> {
        return safeApiCall { api.addWallet(AddWalletRequest(address, type, connector, label)) }
    }

    /**
     * Ensure a wallet exists in user_wallets. Calls addWallet and treats 409 (duplicate) as success.
     * @return null on success, or an error message string on failure.
     */
    suspend fun ensureWalletExists(
        address: String, type: String, connector: String?, label: String?
    ): String? {
        return when (val result = addWallet(address, type, connector, label)) {
            is ApiResult.Success -> {
                Log.d(TAG, "Wallet registered: $address (type=$type)")
                null
            }
            is ApiResult.Error -> {
                if (result.code == ErrorCode.DUPLICATE_WALLET || result.httpCode == 409) {
                    Log.d(TAG, "Wallet already registered: $address (type=$type)")
                    null
                } else {
                    val error = "http=${result.httpCode}, ${result.message}"
                    Log.e(TAG, "Failed to register wallet $address: $error")
                    error
                }
            }
        }
    }

    suspend fun removeWallet(walletId: String): ApiResult<Unit> {
        return safeApiCall { api.removeWallet(walletId) }
    }

    suspend fun setDefaultWallet(walletId: String): ApiResult<Unit> {
        return safeApiCall { api.setDefaultWallet(walletId) }
    }

    /**
     * Update the wallet label on the server (e.g., "External Wallet" → "Phantom").
     * Best-effort — failures are logged but not propagated.
     */
    suspend fun updateWalletLabel(address: String, label: String) {
        when (val result = safeApiCall { api.updateWalletLabel(UpdateWalletLabelRequest(address, label)) }) {
            is ApiResult.Success -> Log.d(TAG, "Updated wallet label for ${address.take(8)}... to '$label'")
            is ApiResult.Error -> Log.w(TAG, "Failed to update wallet label: ${result.message}")
        }
    }
}
