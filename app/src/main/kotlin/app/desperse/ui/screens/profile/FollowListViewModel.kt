package app.desperse.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.data.dto.response.FollowUser
import app.desperse.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FollowListUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val users: List<FollowUser> = emptyList(),
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val followLoadingIds: Set<String> = emptySet()
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowListUiState())
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    val currentUserId: String? get() = userRepository.currentUser.value?.id

    private var currentSlug: String? = null
    private var currentListType: FollowListType? = null

    fun load(slug: String, listType: FollowListType) {
        currentSlug = slug
        currentListType = listType

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, users = emptyList()) }

            val result = when (listType) {
                FollowListType.Followers -> userRepository.getUserFollowers(slug)
                FollowListType.Following -> userRepository.getUserFollowing(slug)
                FollowListType.Collectors -> userRepository.getUserCollectors(slug)
            }

            result
                .onSuccess { data ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        users = data.users,
                        hasMore = data.hasMore,
                        nextCursor = data.nextCursor,
                        error = null
                    ) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load"
                    ) }
                }
        }
    }

    fun loadMore() {
        val slug = currentSlug ?: return
        val listType = currentListType ?: return
        val cursor = _uiState.value.nextCursor ?: return

        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val result = when (listType) {
                FollowListType.Followers -> userRepository.getUserFollowers(slug, cursor)
                FollowListType.Following -> userRepository.getUserFollowing(slug, cursor)
                FollowListType.Collectors -> userRepository.getUserCollectors(slug, cursor)
            }

            result
                .onSuccess { data ->
                    _uiState.update { it.copy(
                        isLoadingMore = false,
                        users = it.users + data.users,
                        hasMore = data.hasMore,
                        nextCursor = data.nextCursor
                    ) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    fun toggleFollow(userId: String) {
        val currentState = _uiState.value
        if (currentState.followLoadingIds.contains(userId)) return

        val user = currentState.users.find { it.id == userId } ?: return
        val isCurrentlyFollowing = user.isFollowing

        viewModelScope.launch {
            _uiState.update { it.copy(
                followLoadingIds = it.followLoadingIds + userId
            ) }

            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    users = state.users.map { u ->
                        if (u.id == userId) u.copy(isFollowing = !isCurrentlyFollowing)
                        else u
                    }
                )
            }

            val result = if (isCurrentlyFollowing) {
                userRepository.unfollowUser(userId)
            } else {
                userRepository.followUser(userId)
            }

            result
                .onSuccess { newIsFollowing ->
                    _uiState.update { state ->
                        state.copy(
                            followLoadingIds = state.followLoadingIds - userId,
                            users = state.users.map { u ->
                                if (u.id == userId) u.copy(isFollowing = newIsFollowing)
                                else u
                            }
                        )
                    }
                }
                .onFailure {
                    // Revert optimistic update
                    _uiState.update { state ->
                        state.copy(
                            followLoadingIds = state.followLoadingIds - userId,
                            users = state.users.map { u ->
                                if (u.id == userId) u.copy(isFollowing = isCurrentlyFollowing)
                                else u
                            }
                        )
                    }
                }
        }
    }
}
