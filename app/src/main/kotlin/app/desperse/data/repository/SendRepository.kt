package app.desperse.data.repository

import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.data.dto.request.PrepareSendRequest
import app.desperse.data.dto.response.PrepareSendResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendRepository @Inject constructor(
    private val api: DesperseApi
) {
    suspend fun prepareSend(
        toAddress: String,
        amount: String,
        asset: String,
        walletAddress: String
    ): Result<PrepareSendResult> {
        return when (val result = safeApiCall {
            api.prepareSend(PrepareSendRequest(toAddress, amount, asset, walletAddress))
        }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }
}
