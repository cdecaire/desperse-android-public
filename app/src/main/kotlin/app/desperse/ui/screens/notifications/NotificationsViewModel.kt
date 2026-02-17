package app.desperse.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.data.dto.response.NotificationItem
import app.desperse.data.repository.NotificationRepository
import app.desperse.data.NotificationCountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val notifications: List<NotificationItem> = emptyList(),
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val isMarkingAllRead: Boolean = false
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val notificationCountManager: NotificationCountManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Initial load of notifications
     */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getNotifications()
                .onSuccess { data ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        notifications = data.notifications,
                        hasMore = data.hasMore,
                        nextCursor = data.nextCursor,
                        error = null
                    ) }
                    // Mark visible notifications as read
                    markVisibleAsRead(data.notifications)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load notifications"
                    ) }
                }
        }
    }

    /**
     * Refresh notifications (pull-to-refresh)
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            repository.getNotifications()
                .onSuccess { data ->
                    _uiState.update { it.copy(
                        isRefreshing = false,
                        notifications = data.notifications,
                        hasMore = data.hasMore,
                        nextCursor = data.nextCursor,
                        error = null
                    ) }
                    // Mark visible notifications as read
                    markVisibleAsRead(data.notifications)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isRefreshing = false,
                        error = error.message ?: "Failed to refresh notifications"
                    ) }
                }
        }
    }

    /**
     * Load more notifications (infinite scroll)
     */
    fun loadMore() {
        val cursor = _uiState.value.nextCursor ?: return
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            repository.getNotifications(cursor)
                .onSuccess { data ->
                    val newNotifications = data.notifications
                    _uiState.update { it.copy(
                        isLoadingMore = false,
                        notifications = it.notifications + newNotifications,
                        hasMore = data.hasMore,
                        nextCursor = data.nextCursor
                    ) }
                    // Mark new notifications as read
                    markVisibleAsRead(newNotifications)
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    /**
     * Mark visible unread notifications as read
     */
    private fun markVisibleAsRead(notifications: List<NotificationItem>) {
        val unreadIds = notifications
            .filter { !it.isRead }
            .map { it.id }

        if (unreadIds.isEmpty()) return

        viewModelScope.launch {
            repository.markAsRead(unreadIds)
                .onSuccess {
                    // Update local state
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { notification ->
                                if (notification.id in unreadIds) {
                                    notification.copy(isRead = true)
                                } else {
                                    notification
                                }
                            }
                        )
                    }
                    // Update global notification count
                    updateGlobalUnreadCount()
                }
        }
    }

    /**
     * Update the global notification count based on local state
     */
    private fun updateGlobalUnreadCount() {
        val unreadCount = _uiState.value.notifications.count { !it.isRead }
        notificationCountManager.setUnreadNotificationCount(unreadCount)
    }

    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        if (_uiState.value.isMarkingAllRead) return

        viewModelScope.launch {
            _uiState.update { it.copy(isMarkingAllRead = true) }

            repository.markAllAsRead()
                .onSuccess {
                    // Update local state
                    _uiState.update { state ->
                        state.copy(
                            isMarkingAllRead = false,
                            notifications = state.notifications.map { it.copy(isRead = true) }
                        )
                    }
                    // Clear global notification count
                    notificationCountManager.clearUnreadNotifications()
                }
                .onFailure {
                    _uiState.update { it.copy(isMarkingAllRead = false) }
                }
        }
    }

    /**
     * Clear all notifications
     */
    fun clearAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.clearAll()
                .onSuccess {
                    _uiState.update { it.copy(
                        isLoading = false,
                        notifications = emptyList(),
                        hasMore = false,
                        nextCursor = null
                    ) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to clear notifications"
                    ) }
                }
        }
    }

    /**
     * Check if there are unread notifications
     */
    fun hasUnread(): Boolean {
        return _uiState.value.notifications.any { !it.isRead }
    }
}
