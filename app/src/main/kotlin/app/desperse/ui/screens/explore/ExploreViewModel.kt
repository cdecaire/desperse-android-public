package app.desperse.ui.screens.explore

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.data.PostUpdate
import app.desperse.data.PostUpdateManager
import app.desperse.data.dto.response.SearchUser
import app.desperse.data.dto.response.SuggestedCreator
import app.desperse.data.model.CollectState
import app.desperse.data.model.Post
import app.desperse.data.repository.ExploreRepository
import app.desperse.data.repository.PostRepository
import app.desperse.core.wallet.TransactionWalletManager
import app.desperse.ui.components.ToastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ExploreViewModel"

data class ExploreUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val suggestedCreators: List<SuggestedCreator> = emptyList(),
    val trendingPosts: List<Post> = emptyList(),
    val sectionTitle: String = "Trending",
    val hasMore: Boolean = false,
    val nextOffset: Int? = null,
    // Search state
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchUsers: List<SearchUser> = emptyList(),
    val searchPosts: List<Post> = emptyList(),
    val showSearchResults: Boolean = false
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val exploreRepository: ExploreRepository,
    private val postRepository: PostRepository,
    private val postUpdateManager: PostUpdateManager,
    private val transactionWalletManager: TransactionWalletManager,
    private val toastManager: ToastManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val _collectStates = MutableStateFlow<Map<String, CollectState>>(emptyMap())
    val collectStates: StateFlow<Map<String, CollectState>> = _collectStates.asStateFlow()

    private var searchJob: Job? = null
    private val pollingJobs = mutableMapOf<String, Job>()

    init {
        load()
        observePostUpdates()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load suggested creators
            val creatorsResult = exploreRepository.getSuggestedCreators()

            // Load trending posts
            val trendingResult = exploreRepository.getTrendingPosts()

            creatorsResult.onSuccess { creators ->
                _uiState.update { it.copy(suggestedCreators = creators) }
            }

            trendingResult
                .onSuccess { data ->
                    postRepository.cachePosts(data.posts)
                    _uiState.update { it.copy(
                        isLoading = false,
                        trendingPosts = data.posts,
                        sectionTitle = data.sectionTitle,
                        hasMore = data.hasMore,
                        nextOffset = data.nextOffset,
                        error = null
                    ) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load explore content"
                    ) }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            val creatorsResult = exploreRepository.getSuggestedCreators()
            val trendingResult = exploreRepository.getTrendingPosts()

            creatorsResult.onSuccess { creators ->
                _uiState.update { it.copy(suggestedCreators = creators) }
            }

            trendingResult
                .onSuccess { data ->
                    postRepository.cachePosts(data.posts)
                    _uiState.update { it.copy(
                        isRefreshing = false,
                        trendingPosts = data.posts,
                        sectionTitle = data.sectionTitle,
                        hasMore = data.hasMore,
                        nextOffset = data.nextOffset,
                        error = null
                    ) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isRefreshing = false,
                        error = error.message ?: "Failed to load explore content"
                    ) }
                }
        }
    }

    fun loadMore() {
        val currentOffset = _uiState.value.nextOffset ?: return
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            exploreRepository.getTrendingPosts(offset = currentOffset)
                .onSuccess { data ->
                    postRepository.cachePosts(data.posts)
                    _uiState.update { it.copy(
                        isLoadingMore = false,
                        trendingPosts = it.trendingPosts + data.posts,
                        hasMore = data.hasMore,
                        nextOffset = data.nextOffset
                    ) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    // === Like ===

    fun likePost(postId: String) {
        viewModelScope.launch {
            val allPosts = _uiState.value.trendingPosts + _uiState.value.searchPosts
            val post = allPosts.find { it.id == postId } ?: return@launch
            val newIsLiked = !post.isLiked
            val newLikeCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1

            // Optimistic update
            applyLikeUpdate(postId, newIsLiked, newLikeCount)
            postUpdateManager.emitLikeUpdate(postId, newIsLiked, newLikeCount)

            // API call
            val result = if (newIsLiked) {
                postRepository.likePost(postId)
            } else {
                postRepository.unlikePost(postId)
            }

            // Revert on failure
            result.onFailure {
                applyLikeUpdate(postId, post.isLiked, post.likeCount)
                postUpdateManager.emitLikeUpdate(postId, post.isLiked, post.likeCount)
            }
        }
    }

    // === Collect ===

    fun collectPost(postId: String) {
        val currentState = _collectStates.value[postId]
        if (currentState is CollectState.Preparing ||
            currentState is CollectState.Confirming ||
            currentState is CollectState.Success) return

        val walletAddress = transactionWalletManager.getActiveWalletAddress()
        Log.d(TAG, "Collecting post $postId with wallet ${walletAddress?.take(8)}...")

        viewModelScope.launch {
            updateCollectState(postId, CollectState.Preparing)

            postRepository.collectPost(postId, walletAddress)
                .onSuccess { result ->
                    when (result.status) {
                        "already_collected" -> {
                            updateCollectState(postId, CollectState.Success)
                            updatePostCollected(postId, true)
                        }
                        "pending" -> {
                            val collectionId = result.collectionId
                            if (collectionId != null) {
                                updateCollectState(postId, CollectState.Confirming(collectionId))
                                startCollectionPolling(postId, collectionId)
                            } else {
                                val message = result.message ?: "Failed to collect"
                                updateCollectState(postId, CollectState.Failed(message))
                                toastManager.showError(message)
                            }
                        }
                        else -> {
                            val message = result.message ?: result.error ?: "Failed to collect"
                            updateCollectState(postId, CollectState.Failed(message))
                            toastManager.showError(message)
                        }
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: "Failed to collect"
                    updateCollectState(postId, CollectState.Failed(message))
                    toastManager.showError(message)
                }
        }
    }

    private fun startCollectionPolling(postId: String, collectionId: String) {
        pollingJobs[postId]?.cancel()

        pollingJobs[postId] = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val maxPollTime = 60_000L
            val pollInterval = 5_000L

            while (System.currentTimeMillis() - startTime < maxPollTime) {
                delay(pollInterval)

                val currentCollectState = _collectStates.value[postId]
                if (currentCollectState !is CollectState.Confirming) return@launch

                postRepository.checkCollectionStatus(collectionId)
                    .onSuccess { status ->
                        when (status.status) {
                            "confirmed" -> {
                                updateCollectState(postId, CollectState.Success)
                                updatePostCollected(postId, true)
                                toastManager.showSuccess("Successfully collected!")
                                pollingJobs.remove(postId)
                                return@launch
                            }
                            "failed" -> {
                                updateCollectState(postId, CollectState.Failed(
                                    status.error ?: "Collection failed"
                                ))
                                toastManager.showError(status.error ?: "Collection failed. You can try again.")
                                pollingJobs.remove(postId)
                                return@launch
                            }
                        }
                    }
            }

            updateCollectState(postId, CollectState.Failed(
                "Still confirming... Check back later.",
                canRetry = false
            ))
            toastManager.showWarning("Taking too long to confirm. Check back later.")
            pollingJobs.remove(postId)
        }
    }

    private fun updateCollectState(postId: String, state: CollectState) {
        _collectStates.update { it + (postId to state) }
    }

    private fun updatePostCollected(postId: String, isCollected: Boolean) {
        _uiState.update { currentState ->
            val post = currentState.trendingPosts.find { it.id == postId }
            val newCollectCount = if (isCollected) (post?.collectCount ?: 0) + 1 else (post?.collectCount ?: 0)
            currentState.copy(
                trendingPosts = currentState.trendingPosts.map { p ->
                    if (p.id == postId) p.copy(isCollected = isCollected, collectCount = newCollectCount) else p
                },
                searchPosts = currentState.searchPosts.map { p ->
                    if (p.id == postId) p.copy(isCollected = isCollected, collectCount = newCollectCount) else p
                }
            )
        }

        viewModelScope.launch {
            val post = _uiState.value.trendingPosts.find { it.id == postId }
            if (post != null) {
                postUpdateManager.emitCollectUpdate(postId, post.isCollected, post.collectCount)
            }
        }
    }

    // === Post Update Sync ===

    private fun observePostUpdates() {
        viewModelScope.launch {
            postUpdateManager.updates.collect { update ->
                when (update) {
                    is PostUpdate.LikeUpdate -> {
                        applyLikeUpdate(update.postId, update.isLiked, update.likeCount)
                    }
                    is PostUpdate.CollectUpdate -> {
                        applyCollectUpdate(update.postId, update.isCollected, update.collectCount, update.currentSupply)
                    }
                    is PostUpdate.CommentCountUpdate -> {
                        applyCommentCountUpdate(update.postId, update.commentCount)
                    }
                    is PostUpdate.PostEdited -> { /* Not needed for explore */ }
                    is PostUpdate.PostCreated, is PostUpdate.PostDeleted -> { /* Not needed for explore */ }
                }
            }
        }
    }

    private fun applyLikeUpdate(postId: String, isLiked: Boolean, likeCount: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                trendingPosts = currentState.trendingPosts.map { post ->
                    if (post.id == postId) post.copy(isLiked = isLiked, likeCount = likeCount) else post
                },
                searchPosts = currentState.searchPosts.map { post ->
                    if (post.id == postId) post.copy(isLiked = isLiked, likeCount = likeCount) else post
                }
            )
        }
    }

    private fun applyCollectUpdate(postId: String, isCollected: Boolean, collectCount: Int, currentSupply: Int? = null) {
        _uiState.update { currentState ->
            currentState.copy(
                trendingPosts = currentState.trendingPosts.map { post ->
                    if (post.id == postId) post.copy(isCollected = isCollected, collectCount = collectCount, currentSupply = currentSupply ?: post.currentSupply) else post
                },
                searchPosts = currentState.searchPosts.map { post ->
                    if (post.id == postId) post.copy(isCollected = isCollected, collectCount = collectCount, currentSupply = currentSupply ?: post.currentSupply) else post
                }
            )
        }
        if (isCollected) {
            _collectStates.update { it + (postId to CollectState.Success) }
        }
    }

    private fun applyCommentCountUpdate(postId: String, commentCount: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                trendingPosts = currentState.trendingPosts.map { post ->
                    if (post.id == postId) post.copy(commentCount = commentCount) else post
                },
                searchPosts = currentState.searchPosts.map { post ->
                    if (post.id == postId) post.copy(commentCount = commentCount) else post
                }
            )
        }
    }

    // === Search ===

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(
                showSearchResults = false,
                searchUsers = emptyList(),
                searchPosts = emptyList()
            ) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true, showSearchResults = true) }

        exploreRepository.search(query)
            .onSuccess { data ->
                _uiState.update { it.copy(
                    isSearching = false,
                    searchUsers = data.users,
                    searchPosts = data.posts
                ) }
            }
            .onFailure {
                _uiState.update { it.copy(isSearching = false) }
            }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(
            searchQuery = "",
            showSearchResults = false,
            searchUsers = emptyList(),
            searchPosts = emptyList()
        ) }
    }

    /**
     * Report a post
     */
    suspend fun createReport(
        contentType: String,
        contentId: String,
        reasons: List<String>,
        details: String?
    ): Result<Unit> {
        return postRepository.createReport(contentType, contentId, reasons, details)
            .map { }
            .also { result ->
                result.onSuccess {
                    toastManager.showSuccess("Report submitted. Thanks for helping keep the community safe.")
                }
            }
    }
}
