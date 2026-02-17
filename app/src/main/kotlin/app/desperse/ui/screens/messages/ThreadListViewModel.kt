package app.desperse.ui.screens.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.realtime.AblyEvent
import app.desperse.core.realtime.AblyManager
import app.desperse.data.UnreadMessageManager
import app.desperse.data.dto.response.DmEligibilityResponse
import app.desperse.data.dto.response.MentionUser
import app.desperse.data.dto.response.ThreadResponse
import app.desperse.data.repository.MessageRepository
import app.desperse.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThreadListUiState(
    val threads: List<ThreadResponse> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val lastFetchTime: Long = 0L
)

data class NewMessageUiState(
    val searchQuery: String = "",
    val searchResults: List<MentionUser> = emptyList(),
    val isSearching: Boolean = false,
    val selectedUser: MentionUser? = null,
    val eligibility: DmEligibilityResponse? = null,
    val isCheckingEligibility: Boolean = false,
    val isCreatingThread: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ThreadListViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val postRepository: PostRepository,
    private val ablyManager: AblyManager,
    private val unreadMessageManager: UnreadMessageManager
) : ViewModel() {

    companion object {
        /** How long before data is considered stale (30 seconds, matching web app) */
        private const val STALE_TIME_MS = 30_000L
        /** Interval for periodic silent refresh when screen is active (45 seconds) */
        private const val REFRESH_INTERVAL_MS = 45_000L
    }

    private val _uiState = MutableStateFlow(ThreadListUiState())
    val uiState: StateFlow<ThreadListUiState> = _uiState.asStateFlow()

    private val _newMessageState = MutableStateFlow(NewMessageUiState())
    val newMessageState: StateFlow<NewMessageUiState> = _newMessageState.asStateFlow()

    private var nextCursor: String? = null
    private var refreshJob: Job? = null
    private var searchJob: Job? = null

    /** Unread count for badge display (expose for bottom nav) */
    val unreadCount: StateFlow<Int> = _uiState.map { state ->
        state.threads.count { it.hasUnread }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadThreads()
        observeAblyEvents()
    }

    private fun observeAblyEvents() {
        viewModelScope.launch {
            ablyManager.events.collect { event ->
                when (event) {
                    is AblyEvent.NewMessage -> {
                        // Move thread to top with unread, or refresh if unknown thread
                        val threadIndex = _uiState.value.threads.indexOfFirst { it.id == event.threadId }
                        if (threadIndex >= 0) {
                            _uiState.update { state ->
                                val threads = state.threads.toMutableList()
                                val thread = threads.removeAt(threadIndex)
                                threads.add(0, thread.copy(hasUnread = true))
                                state.copy(threads = threads)
                            }
                        } else {
                            silentRefresh()
                        }
                    }
                    is AblyEvent.MessageRead -> {
                        _uiState.update { state ->
                            state.copy(
                                threads = state.threads.map { thread ->
                                    if (thread.id == event.threadId) thread.copy(hasUnread = false)
                                    else thread
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadThreads() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            nextCursor = null

            messageRepository.getThreads(limit = 20)
                .onSuccess { response ->
                    nextCursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            threads = response.threads,
                            isLoading = false,
                            hasMore = response.nextCursor != null,
                            lastFetchTime = System.currentTimeMillis()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            nextCursor = null

            messageRepository.getThreads(limit = 20)
                .onSuccess { response ->
                    nextCursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            threads = response.threads,
                            isRefreshing = false,
                            hasMore = response.nextCursor != null,
                            lastFetchTime = System.currentTimeMillis(),
                            error = null
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
        }
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (_uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            messageRepository.getThreads(cursor = cursor, limit = 20)
                .onSuccess { response ->
                    nextCursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            threads = it.threads + response.threads,
                            isLoadingMore = false,
                            hasMore = response.nextCursor != null
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    /**
     * Called when the messages screen becomes visible.
     * Starts periodic refresh and does an immediate refresh if data is stale.
     */
    fun onScreenVisible() {
        // Always silent refresh when becoming visible — picks up read state
        // changes from ConversationViewModel without showing loading indicators
        if (_uiState.value.threads.isNotEmpty()) {
            silentRefresh()
        }
        startPeriodicRefresh()
        // Sync badge state with current thread data
        syncUnreadBadge()
    }

    /** Push current thread unread state to the global badge manager */
    private fun syncUnreadBadge() {
        viewModelScope.launch {
            unreadMessageManager.refreshUnreadStatus()
        }
    }

    /**
     * Called when the messages screen is no longer visible.
     * Stops periodic refresh to save resources.
     */
    fun onScreenHidden() {
        stopPeriodicRefresh()
    }

    /**
     * Silent refresh - updates data without showing loading indicators.
     * Used for periodic background updates to keep the thread list fresh.
     */
    private fun silentRefresh() {
        viewModelScope.launch {
            messageRepository.getThreads(limit = 20)
                .onSuccess { response ->
                    nextCursor = response.nextCursor
                    _uiState.update {
                        it.copy(
                            threads = response.threads,
                            hasMore = response.nextCursor != null,
                            lastFetchTime = System.currentTimeMillis()
                        )
                    }
                }
            // Silently ignore errors - don't disrupt the UI
        }
    }

    private fun startPeriodicRefresh() {
        stopPeriodicRefresh()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
                if (_uiState.value.threads.isNotEmpty() &&
                    !_uiState.value.isLoading &&
                    !_uiState.value.isRefreshing
                ) {
                    silentRefresh()
                }
            }
        }
    }

    private fun stopPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    // --- New Message ---

    fun searchUsers(query: String) {
        _newMessageState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.length < 2) {
            _newMessageState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _newMessageState.update { it.copy(isSearching = true) }
            postRepository.searchMentionUsers(query)
                .onSuccess { users ->
                    _newMessageState.update { it.copy(searchResults = users, isSearching = false) }
                }
                .onFailure { error ->
                    _newMessageState.update { it.copy(isSearching = false, error = error.message) }
                }
        }
    }

    fun selectUser(user: MentionUser) {
        _newMessageState.update {
            it.copy(
                selectedUser = user,
                eligibility = null,
                isCheckingEligibility = true,
                error = null
            )
        }
        viewModelScope.launch {
            messageRepository.checkDmEligibility(user.id)
                .onSuccess { eligibility ->
                    _newMessageState.update {
                        it.copy(eligibility = eligibility, isCheckingEligibility = false)
                    }
                }
                .onFailure { error ->
                    _newMessageState.update {
                        it.copy(isCheckingEligibility = false, error = error.message)
                    }
                }
        }
    }

    fun clearSelectedUser() {
        _newMessageState.update {
            it.copy(selectedUser = null, eligibility = null, isCheckingEligibility = false, error = null)
        }
    }

    fun startConversation(onSuccess: (threadId: String, name: String?, slug: String?, avatar: String?) -> Unit) {
        val user = _newMessageState.value.selectedUser ?: return
        if (_newMessageState.value.isCreatingThread) return

        _newMessageState.update { it.copy(isCreatingThread = true, error = null) }
        viewModelScope.launch {
            messageRepository.getOrCreateThread(
                otherUserId = user.id,
                contextCreatorId = user.id
            )
                .onSuccess { response ->
                    _newMessageState.update { it.copy(isCreatingThread = false) }
                    onSuccess(
                        response.thread.id,
                        user.displayName,
                        user.usernameSlug,
                        user.avatarUrl
                    )
                }
                .onFailure { error ->
                    _newMessageState.update {
                        it.copy(isCreatingThread = false, error = error.message)
                    }
                }
        }
    }

    fun resetNewMessage() {
        searchJob?.cancel()
        _newMessageState.value = NewMessageUiState()
    }

    override fun onCleared() {
        super.onCleared()
        stopPeriodicRefresh()
    }
}
