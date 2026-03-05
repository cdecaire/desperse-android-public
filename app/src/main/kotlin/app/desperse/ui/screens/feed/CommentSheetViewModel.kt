package app.desperse.ui.screens.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.data.PostUpdateManager
import app.desperse.data.dto.response.Comment
import app.desperse.data.dto.response.MentionUser
import app.desperse.data.repository.PostRepository
import app.desperse.data.repository.UserRepository
import app.desperse.ui.components.ToastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommentSheetUiState(
    val postId: String? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmitting: Boolean = false,
    val commentError: String? = null,
    val deletingCommentId: String? = null,
    val currentUserId: String? = null,
    val currentUserAvatarUrl: String? = null,
    val currentUserSlug: String? = null,
    val commentCount: Int = 0
)

@HiltViewModel
class CommentSheetViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val postUpdateManager: PostUpdateManager,
    private val userRepository: UserRepository,
    private val toastManager: ToastManager
) : ViewModel() {

    companion object {
        private const val TAG = "CommentSheetVM"
    }

    private val _uiState = MutableStateFlow(CommentSheetUiState())
    val uiState: StateFlow<CommentSheetUiState> = _uiState.asStateFlow()

    private var loadCommentsJob: Job? = null

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    currentUserId = user?.id,
                    currentUserAvatarUrl = user?.avatarUrl,
                    currentUserSlug = user?.slug
                )
            }
        }
    }

    /**
     * Open the sheet for a post. Loads comments if switching to a new post
     * or if no comments have been loaded yet.
     */
    fun openForPost(postId: String, commentCount: Int) {
        val current = _uiState.value
        // Skip re-fetch if same post with existing comments
        if (current.postId == postId && current.comments.isNotEmpty()) {
            _uiState.value = current.copy(commentCount = commentCount)
            return
        }

        // Cancel any in-flight load
        loadCommentsJob?.cancel()

        _uiState.value = _uiState.value.copy(
            postId = postId,
            comments = emptyList(),
            isLoading = true,
            error = null,
            commentCount = commentCount,
            commentError = null,
            deletingCommentId = null
        )

        loadCommentsJob = viewModelScope.launch {
            postRepository.getComments(postId)
                .onSuccess { comments ->
                    _uiState.value = _uiState.value.copy(
                        comments = comments,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load comments: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load comments"
                    )
                }
        }
    }

    fun createComment(content: String) {
        val postId = _uiState.value.postId ?: return
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty() || trimmedContent.length > 280) return
        if (_uiState.value.isSubmitting) return

        _uiState.value = _uiState.value.copy(isSubmitting = true, commentError = null)

        viewModelScope.launch {
            postRepository.createComment(postId, trimmedContent)
                .onSuccess { newComment ->
                    val updatedComments = listOf(newComment) + _uiState.value.comments
                    val newCount = _uiState.value.commentCount + 1

                    _uiState.value = _uiState.value.copy(
                        comments = updatedComments,
                        isSubmitting = false,
                        commentCount = newCount
                    )

                    postUpdateManager.emitCommentCountUpdate(postId, newCount)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        commentError = error.message ?: "Failed to post comment"
                    )
                }
        }
    }

    /**
     * Delete a comment with optimistic update and rollback on failure.
     */
    fun deleteComment(commentId: String) {
        val postId = _uiState.value.postId ?: return
        val currentUserId = _uiState.value.currentUserId ?: return
        val comments = _uiState.value.comments
        val commentToDelete = comments.find { it.id == commentId } ?: return

        if (commentToDelete.user.id != currentUserId) {
            Log.w(TAG, "Attempted to delete comment owned by another user")
            return
        }
        if (_uiState.value.deletingCommentId != null) return

        // Optimistic: remove immediately
        val updatedComments = comments.filter { it.id != commentId }
        val newCount = (_uiState.value.commentCount - 1).coerceAtLeast(0)

        _uiState.value = _uiState.value.copy(
            comments = updatedComments,
            deletingCommentId = commentId,
            commentCount = newCount
        )

        viewModelScope.launch {
            postUpdateManager.emitCommentCountUpdate(postId, newCount)

            postRepository.deleteComment(postId, commentId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(deletingCommentId = null)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to delete comment: ${error.message}")
                    val restoredCount = _uiState.value.commentCount + 1
                    _uiState.value = _uiState.value.copy(
                        comments = comments,
                        deletingCommentId = null,
                        commentCount = restoredCount,
                        commentError = error.message ?: "Failed to delete comment"
                    )
                    postUpdateManager.emitCommentCountUpdate(postId, restoredCount)
                }
        }
    }

    suspend fun searchMentionUsers(query: String): List<MentionUser> {
        return postRepository.searchMentionUsers(query.ifEmpty { null })
            .getOrElse { emptyList() }
    }

    fun clearCommentError() {
        _uiState.value = _uiState.value.copy(commentError = null)
    }

    fun clearState() {
        loadCommentsJob?.cancel()
        _uiState.value = _uiState.value.copy(
            postId = null,
            comments = emptyList(),
            isLoading = false,
            error = null,
            isSubmitting = false,
            commentError = null,
            deletingCommentId = null,
            commentCount = 0
        )
    }
}
