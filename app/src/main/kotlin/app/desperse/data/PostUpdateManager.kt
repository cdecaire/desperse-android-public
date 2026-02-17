package app.desperse.data

import app.desperse.data.model.Post
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages post state updates across screens.
 *
 * When a post is modified (liked, collected, etc.) in one screen,
 * this manager broadcasts the update so other screens can sync their state.
 */
@Singleton
class PostUpdateManager @Inject constructor() {

    private val _updates = MutableSharedFlow<PostUpdate>(extraBufferCapacity = 10)
    val updates: SharedFlow<PostUpdate> = _updates.asSharedFlow()

    /**
     * Emit a like state change for a post
     */
    suspend fun emitLikeUpdate(postId: String, isLiked: Boolean, likeCount: Int) {
        _updates.emit(PostUpdate.LikeUpdate(postId, isLiked, likeCount))
    }

    /**
     * Emit a collect state change for a post
     */
    suspend fun emitCollectUpdate(postId: String, isCollected: Boolean, collectCount: Int) {
        _updates.emit(PostUpdate.CollectUpdate(postId, isCollected, collectCount))
    }

    /**
     * Emit a comment count change for a post
     */
    suspend fun emitCommentCountUpdate(postId: String, commentCount: Int) {
        _updates.emit(PostUpdate.CommentCountUpdate(postId, commentCount))
    }

    /**
     * Emit that a new post was created (feed should refresh)
     */
    suspend fun emitPostCreated(postId: String) {
        _updates.emit(PostUpdate.PostCreated(postId))
    }

    /**
     * Emit that a post was deleted (remove from feed)
     */
    suspend fun emitPostDeleted(postId: String) {
        _updates.emit(PostUpdate.PostDeleted(postId))
    }

    /**
     * Emit that a post was edited (update in feed/profile/detail)
     */
    suspend fun emitPostEdited(post: Post) {
        _updates.emit(PostUpdate.PostEdited(post.id, post))
    }
}

/**
 * Sealed class representing different types of post updates
 */
sealed class PostUpdate {
    abstract val postId: String

    data class LikeUpdate(
        override val postId: String,
        val isLiked: Boolean,
        val likeCount: Int
    ) : PostUpdate()

    data class CollectUpdate(
        override val postId: String,
        val isCollected: Boolean,
        val collectCount: Int
    ) : PostUpdate()

    data class CommentCountUpdate(
        override val postId: String,
        val commentCount: Int
    ) : PostUpdate()

    data class PostCreated(
        override val postId: String
    ) : PostUpdate()

    data class PostDeleted(
        override val postId: String
    ) : PostUpdate()

    data class PostEdited(
        override val postId: String,
        val post: Post
    ) : PostUpdate()
}
