package app.desperse.data.repository

import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.data.dto.request.ArchiveRequest
import app.desperse.data.dto.request.BlockRequest
import app.desperse.data.dto.request.CreateThreadRequest
import app.desperse.data.dto.request.SendMessageRequest
import app.desperse.data.dto.request.UpdateDmPreferencesRequest
import app.desperse.data.dto.response.AblyTokenResponse
import app.desperse.data.dto.response.ArchiveResponse
import app.desperse.data.dto.response.BlockResponse
import app.desperse.data.dto.response.CreateThreadResponse
import app.desperse.data.dto.response.DmEligibilityResponse
import app.desperse.data.dto.response.DmPreferencesResponse
import app.desperse.data.dto.response.MarkReadResponse
import app.desperse.data.dto.response.MessagesListResponse
import app.desperse.data.dto.response.SendMessageResponse
import app.desperse.data.dto.response.ThreadListResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val api: DesperseApi
) {
    suspend fun getThreads(cursor: String? = null, limit: Int? = null): Result<ThreadListResponse> {
        return when (val result = safeApiCall { api.getThreads(cursor, limit) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun getOrCreateThread(otherUserId: String, contextCreatorId: String): Result<CreateThreadResponse> {
        return when (val result = safeApiCall { api.getOrCreateThread(CreateThreadRequest(otherUserId, contextCreatorId)) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun getMessages(threadId: String, cursor: String? = null, limit: Int? = null): Result<MessagesListResponse> {
        return when (val result = safeApiCall { api.getMessages(threadId, cursor, limit) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun sendMessage(threadId: String, content: String): Result<SendMessageResponse> {
        return when (val result = safeApiCall { api.sendMessage(threadId, SendMessageRequest(content)) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun markThreadRead(threadId: String): Result<MarkReadResponse> {
        return when (val result = safeApiCall { api.markThreadRead(threadId) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun blockInThread(threadId: String, blocked: Boolean): Result<BlockResponse> {
        return when (val result = safeApiCall { api.blockInThread(threadId, BlockRequest(blocked)) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun archiveThread(threadId: String, archived: Boolean): Result<ArchiveResponse> {
        return when (val result = safeApiCall { api.archiveThread(threadId, ArchiveRequest(archived)) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return when (val result = safeApiCall { api.deleteMessage(messageId) }) {
            is ApiResult.Success -> Result.success(Unit)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun checkDmEligibility(creatorId: String): Result<DmEligibilityResponse> {
        return when (val result = safeApiCall { api.checkDmEligibility(creatorId) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun getDmPreferences(): Result<DmPreferencesResponse> {
        return when (val result = safeApiCall { api.getDmPreferences() }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun updateDmPreferences(
        dmEnabled: Boolean? = null,
        allowBuyers: Boolean? = null,
        allowCollectors: Boolean? = null,
        collectorMinCount: Int? = null,
        allowTippers: Boolean? = null,
        tipMinAmount: Int? = null
    ): Result<DmPreferencesResponse> {
        return when (val result = safeApiCall {
            api.updateDmPreferences(UpdateDmPreferencesRequest(dmEnabled, allowBuyers, allowCollectors, collectorMinCount, allowTippers, tipMinAmount))
        }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun getAblyToken(): Result<AblyTokenResponse> {
        return when (val result = safeApiCall { api.getAblyToken() }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }
}
