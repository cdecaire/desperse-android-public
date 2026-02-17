package app.desperse.data.repository

import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.data.dto.request.ConfirmTipRequest
import app.desperse.data.dto.request.PrepareTipRequest
import app.desperse.data.dto.response.ConfirmTipResult
import app.desperse.data.dto.response.PrepareTipResult
import app.desperse.data.dto.response.TipStatsResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TipRepository @Inject constructor(
    private val api: DesperseApi
) {
    suspend fun prepareTip(
        toUserId: String,
        amount: Double,
        context: String,
        walletAddress: String? = null
    ): Result<PrepareTipResult> {
        return when (val result = safeApiCall {
            api.prepareTip(PrepareTipRequest(toUserId, amount, context, walletAddress))
        }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun confirmTip(tipId: String, txSignature: String): Result<ConfirmTipResult> {
        return when (val result = safeApiCall {
            api.confirmTip(ConfirmTipRequest(tipId, txSignature))
        }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun getTipStats(userId: String): Result<TipStatsResult> {
        return when (val result = safeApiCall { api.getTipStats(userId) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }
}
