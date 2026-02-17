package app.desperse.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.data.dto.response.SearchUser
import app.desperse.data.dto.response.SuggestedCreator
import app.desperse.data.model.Post
import app.desperse.data.repository.ExploreRepository
import app.desperse.data.repository.PostRepository
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

data class ExploreUiState(
    val isLoading: Boolean = false,
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
    private val toastManager: ToastManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        load()
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

    fun loadMore() {
        val currentOffset = _uiState.value.nextOffset ?: return
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            exploreRepository.getTrendingPosts(offset = currentOffset)
                .onSuccess { data ->
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

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        // Cancel previous search
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(
                showSearchResults = false,
                searchUsers = emptyList(),
                searchPosts = emptyList()
            ) }
            return
        }

        // Debounce search
        searchJob = viewModelScope.launch {
            delay(300) // 300ms debounce
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
            .map { }  // Convert Result<String> to Result<Unit>
            .also { result ->
                result.onSuccess {
                    toastManager.showSuccess("Report submitted. Thanks for helping keep the community safe.")
                }
            }
    }
}
