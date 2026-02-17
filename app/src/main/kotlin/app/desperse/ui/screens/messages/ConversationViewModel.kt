package app.desperse.ui.screens.messages

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.realtime.AblyEvent
import app.desperse.core.realtime.AblyManager
import app.desperse.data.dto.response.MessageResponse
import app.desperse.data.UnreadMessageManager
import app.desperse.data.repository.MessageRepository
import app.desperse.data.repository.PostRepository
import app.desperse.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val TAG = "ConversationViewModel"

data class ConversationUiState(
    val messages: List<MessageResponse> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val otherLastReadAt: String? = null,
    val isBlocked: Boolean = false,
    val isBlockedBy: Boolean = false,
    val currentUserId: String? = null,
    val currentUserAvatarUrl: String? = null,
    val currentUserSlug: String? = null,
    val otherUserName: String? = null,
    val otherUserSlug: String? = null,
    val otherUserAvatarUrl: String? = null
)

@HiltViewModel
class ConversationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val ablyManager: AblyManager,
    private val unreadMessageManager: UnreadMessageManager
) : ViewModel() {

    val threadId: String = savedStateHandle.get<String>("threadId") ?: ""

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    private var nextCursor: String? = null
    private var markReadJob: Job? = null

    init {
        observeCurrentUser()
        loadMessages()
        observeAblyEvents()
    }

    private fun observeAblyEvents() {
        viewModelScope.launch {
            ablyManager.events.collect { event ->
                when (event) {
                    is AblyEvent.NewMessage -> {
                        if (event.threadId == threadId) {
                            // New message in this thread — refresh to get content
                            refreshLatestMessages()
                            markRead()
                        }
                    }
                    is AblyEvent.MessageRead -> {
                        if (event.threadId == threadId) {
                            _uiState.update { it.copy(otherLastReadAt = event.readAt) }
                        }
                    }
                }
            }
        }
    }

    private fun refreshLatestMessages() {
        viewModelScope.launch {
            messageRepository.getMessages(threadId, limit = 50)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            messages = response.messages.reversed(),
                            otherLastReadAt = response.otherLastReadAt
                        )
                    }
                }
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUserId = user?.id, currentUserAvatarUrl = user?.avatarUrl, currentUserSlug = user?.slug) }
            }
        }
    }

    fun loadMessages() {
        if (threadId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            messageRepository.getMessages(threadId, limit = 50)
                .onSuccess { response ->
                    nextCursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            // Messages come newest-first from API, reverse for display (oldest at top)
                            messages = response.messages.reversed(),
                            isLoading = false,
                            hasMore = response.nextCursor != null,
                            otherLastReadAt = response.otherLastReadAt
                        )
                    }
                    // Mark thread as read
                    markRead()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load messages: ${error.message}")
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun loadOlderMessages() {
        val cursor = nextCursor ?: return
        if (_uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            messageRepository.getMessages(threadId, cursor = cursor, limit = 50)
                .onSuccess { response ->
                    nextCursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            // Prepend older messages at the beginning
                            messages = response.messages.reversed() + it.messages,
                            isLoadingMore = false,
                            hasMore = response.nextCursor != null
                        )
                    }
                }
                .onFailure {
                    Log.e(TAG, "Failed to load older messages: ${it.message}")
                    _uiState.update { state -> state.copy(isLoadingMore = false) }
                }
        }
    }

    fun sendMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank() || trimmed.length > 2000) return
        if (_uiState.value.isSending) return

        val currentUserId = _uiState.value.currentUserId ?: return

        // Optimistic: add message to list immediately
        val tempId = "temp_${UUID.randomUUID()}"
        val optimisticMessage = MessageResponse(
            id = tempId,
            threadId = threadId,
            senderId = currentUserId,
            content = trimmed,
            isDeleted = false,
            createdAt = java.time.Instant.now().toString()
        )

        _uiState.update {
            it.copy(
                messages = it.messages + optimisticMessage,
                isSending = true
            )
        }

        viewModelScope.launch {
            messageRepository.sendMessage(threadId, trimmed)
                .onSuccess { response ->
                    // Replace optimistic message with real one
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == tempId) response.message else msg
                            },
                            isSending = false
                        )
                    }
                }
                .onFailure {
                    Log.e(TAG, "Failed to send message: ${it.message}")
                    // Remove optimistic message on failure
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.filter { msg -> msg.id != tempId },
                            isSending = false
                        )
                    }
                }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == messageId) msg.copy(isDeleted = true, content = "") else msg
                            }
                        )
                    }
                }
                .onFailure {
                    Log.e(TAG, "Failed to delete message: ${it.message}")
                }
        }
    }

    fun blockUser(blocked: Boolean) {
        viewModelScope.launch {
            messageRepository.blockInThread(threadId, blocked)
                .onSuccess {
                    _uiState.update { it.copy(isBlocked = blocked) }
                }
                .onFailure {
                    Log.e(TAG, "Failed to block/unblock: ${it.message}")
                }
        }
    }

    fun archiveThread() {
        viewModelScope.launch {
            messageRepository.archiveThread(threadId, true)
        }
    }

    suspend fun createReport(
        reasons: List<String>,
        details: String?
    ): Result<Unit> {
        return postRepository.createReport("dm_thread", threadId, reasons, details)
            .map { }
    }

    private var hasMarkedRead = false

    private fun markRead() {
        if (hasMarkedRead) return
        markReadJob?.cancel()
        markReadJob = viewModelScope.launch {
            delay(2000) // Debounce — confirm user is actually reading
            hasMarkedRead = true
            // Fire on manager's scope so it survives ViewModel destruction
            unreadMessageManager.markThreadRead(threadId)
        }
    }

    fun onScreenVisible() {
        markRead()
    }

    fun onScreenHidden() {
        // If the debounce hasn't fired yet but the user viewed the screen,
        // mark as read immediately before leaving
        if (!hasMarkedRead) {
            markReadJob?.cancel()
            hasMarkedRead = true
            unreadMessageManager.markThreadRead(threadId)
        }
    }

    /**
     * Set thread metadata (called from screen when navigating with extra data).
     */
    fun setThreadInfo(
        otherUserName: String?,
        otherUserSlug: String?,
        otherUserAvatarUrl: String?,
        isBlocked: Boolean,
        isBlockedBy: Boolean
    ) {
        _uiState.update {
            it.copy(
                otherUserName = otherUserName,
                otherUserSlug = otherUserSlug,
                otherUserAvatarUrl = otherUserAvatarUrl,
                isBlocked = isBlocked,
                isBlockedBy = isBlockedBy
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        markReadJob?.cancel()
    }
}
