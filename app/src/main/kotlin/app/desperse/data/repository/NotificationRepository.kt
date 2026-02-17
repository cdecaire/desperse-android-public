package app.desperse.data.repository

import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.data.dto.request.MarkNotificationsReadRequest
import app.desperse.data.dto.response.NotificationItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result for paginated notifications
 */
data class NotificationsPage(
    val notifications: List<NotificationItem>,
    val nextCursor: String?,
    val hasMore: Boolean
)

@Singleton
class NotificationRepository @Inject constructor(
    private val api: DesperseApi
) {
    /**
     * Get paginated notifications for the current user
     */
    suspend fun getNotifications(cursor: String? = null, limit: Int = 20): Result<NotificationsPage> {
        return when (val result = safeApiCall { api.getNotifications(cursor, limit) }) {
            is ApiResult.Success -> Result.success(
                NotificationsPage(
                    notifications = result.data.notifications,
                    nextCursor = result.meta?.nextCursor,
                    hasMore = result.meta?.hasMore ?: false
                )
            )
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    /**
     * Mark specific notifications as read
     */
    suspend fun markAsRead(notificationIds: List<String>): Result<Int> {
        if (notificationIds.isEmpty()) return Result.success(0)

        return when (val result = safeApiCall {
            api.markNotificationsRead(MarkNotificationsReadRequest(notificationIds))
        }) {
            is ApiResult.Success -> Result.success(result.data.markedCount)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    /**
     * Mark all notifications as read
     */
    suspend fun markAllAsRead(): Result<Unit> {
        return when (val result = safeApiCall { api.markAllNotificationsRead() }) {
            is ApiResult.Success -> Result.success(Unit)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    /**
     * Clear all notifications
     */
    suspend fun clearAll(): Result<Unit> {
        return when (val result = safeApiCall { api.clearAllNotifications() }) {
            is ApiResult.Success -> Result.success(Unit)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }
}
