package app.desperse.data.repository

import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.data.dto.request.BuyEditionRequest
import app.desperse.data.dto.request.CollectRequest
import app.desperse.data.dto.request.CreateCommentRequest
import app.desperse.data.dto.request.CreateFeedbackRequest
import app.desperse.data.dto.request.CreatePostRequest
import app.desperse.data.dto.request.SubmitSignedTxRequest
import app.desperse.data.dto.request.UpdatePostRequest
import app.desperse.data.dto.request.MediaUploadRequest
import app.desperse.data.dto.request.UploadTokenRequest
import app.desperse.data.dto.response.MediaUploadResult
import app.desperse.data.dto.response.BuyEditionResult
import app.desperse.data.dto.response.CollectResult
import app.desperse.data.dto.response.CollectionStatusResult
import app.desperse.data.dto.response.Comment
import app.desperse.data.dto.request.CreateReportRequest
import app.desperse.data.dto.response.DeletePostResult
import app.desperse.data.dto.response.EditStateResult
import app.desperse.data.dto.response.MentionUser
import app.desperse.data.dto.response.PurchaseStatusResult
import app.desperse.data.dto.response.SubmitResult
import app.desperse.data.model.Post
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val api: DesperseApi
) {
    suspend fun getFeed(tab: String, cursor: String? = null, limit: Int = 20): Result<List<Post>> {
        return when (val result = safeApiCall { api.getPosts(tab, cursor, limit) }) {
            is ApiResult.Success -> Result.success(result.data.posts)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun getPost(postId: String): Result<Post> {
        return when (val result = safeApiCall { api.getPost(postId) }) {
            is ApiResult.Success -> Result.success(result.data.post)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun likePost(postId: String): Result<Boolean> {
        return when (val result = safeApiCall { api.likePost(postId) }) {
            is ApiResult.Success -> Result.success(result.data.isLiked)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun unlikePost(postId: String): Result<Boolean> {
        return when (val result = safeApiCall { api.unlikePost(postId) }) {
            is ApiResult.Success -> Result.success(result.data.isLiked)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    /**
     * Collect a free collectible post. The cNFT will be minted to the specified wallet.
     * @param walletAddress Optional wallet address to mint to. If null, server uses the DB default.
     */
    suspend fun collectPost(postId: String, walletAddress: String? = null): Result<CollectResult> {
        val idempotencyKey = UUID.randomUUID().toString()
        return when (val result = safeApiCall { api.collectPost(postId, idempotencyKey, CollectRequest(walletAddress)) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun checkCollectionStatus(collectionId: String): Result<CollectionStatusResult> {
        return when (val result = safeApiCall { api.checkCollectionStatus(collectionId) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun getComments(postId: String, limit: Int = 50, cursor: String? = null): Result<List<Comment>> {
        return when (val result = safeApiCall { api.getComments(postId, limit, cursor) }) {
            is ApiResult.Success -> Result.success(result.data.comments)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun createComment(postId: String, content: String): Result<Comment> {
        return when (val result = safeApiCall { api.createComment(postId, CreateCommentRequest(content)) }) {
            is ApiResult.Success -> Result.success(result.data.comment)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Boolean> {
        return when (val result = safeApiCall { api.deleteComment(postId, commentId) }) {
            is ApiResult.Success -> Result.success(result.data.deleted)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    // === Edition Purchase Flow ===

    /**
     * Step 1: Request to buy an edition - server returns unsigned transaction.
     * @param walletAddress Optional wallet address to build the transaction for.
     *   If null, server uses the user's default DB wallet.
     */
    suspend fun buyEdition(postId: String, walletAddress: String? = null): Result<BuyEditionResult> {
        val idempotencyKey = UUID.randomUUID().toString()
        return when (val result = safeApiCall { api.buyEdition(BuyEditionRequest(postId, walletAddress), idempotencyKey) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    /**
     * Step 2: Submit transaction signature back to server after broadcasting
     */
    suspend fun submitPurchaseSignature(purchaseId: String, txSignature: String): Result<SubmitResult> {
        val request = SubmitSignedTxRequest(purchaseId, txSignature)
        return when (val result = safeApiCall { api.submitSignedTransaction(request) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    /**
     * Step 3: Poll for purchase status until confirmed/failed
     */
    suspend fun checkPurchaseStatus(purchaseId: String): Result<PurchaseStatusResult> {
        return when (val result = safeApiCall { api.checkPurchaseStatus(purchaseId) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    // === Mention Search ===

    /**
     * Search users for @mention autocomplete
     */
    suspend fun searchMentionUsers(query: String?, limit: Int = 8): Result<List<MentionUser>> {
        return when (val result = safeApiCall { api.searchMentionUsers(query, limit) }) {
            is ApiResult.Success -> Result.success(result.data.users)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    // === Reports ===

    /**
     * Report a post or comment
     * @param contentType "post" or "comment"
     * @param contentId The ID of the content being reported
     * @param reasons List of reason strings (at least one required)
     * @param details Optional additional details (max 500 chars)
     */
    suspend fun createReport(
        contentType: String,
        contentId: String,
        reasons: List<String>,
        details: String? = null
    ): Result<String> {
        val request = CreateReportRequest(
            contentType = contentType,
            contentId = contentId,
            reasons = reasons,
            details = details
        )
        return when (val result = safeApiCall { api.createReport(request) }) {
            is ApiResult.Success -> Result.success(result.data.reportId)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    // === Feedback ===

    /**
     * Submit beta feedback
     * Requires at least one of: rating, message, or imageUrl
     */
    suspend fun createFeedback(
        rating: Int? = null,
        message: String? = null,
        imageUrl: String? = null,
        pageUrl: String? = null,
        appVersion: String? = null,
        userAgent: String? = null
    ): Result<String> {
        val request = CreateFeedbackRequest(
            rating = rating,
            message = message,
            imageUrl = imageUrl,
            pageUrl = pageUrl,
            appVersion = appVersion,
            userAgent = userAgent
        )
        return when (val result = safeApiCall { api.createFeedback(request) }) {
            is ApiResult.Success -> Result.success(result.data.id)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    // === Post CRUD ===

    suspend fun createPost(request: CreatePostRequest): Result<Post> {
        return when (val result = safeApiCall { api.createPost(request) }) {
            is ApiResult.Success -> Result.success(result.data.post)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun updatePost(postId: String, request: UpdatePostRequest): Result<Post> {
        return when (val result = safeApiCall { api.updatePost(postId, request) }) {
            is ApiResult.Success -> Result.success(result.data.post)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun deletePost(postId: String): Result<DeletePostResult> {
        return when (val result = safeApiCall { api.deletePost(postId) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun getEditState(postId: String): Result<EditStateResult> {
        return when (val result = safeApiCall { api.getEditState(postId) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun getUploadToken(pathname: String, contentType: String, fileSize: Long? = null): Result<String> {
        val request = UploadTokenRequest(pathname, contentType, fileSize)
        return when (val result = safeApiCall { api.getUploadToken(request) }) {
            is ApiResult.Success -> Result.success(result.data.clientToken)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }

    suspend fun uploadMedia(
        base64Data: String,
        fileName: String,
        mimeType: String,
        fileSize: Long
    ): Result<MediaUploadResult> {
        val request = MediaUploadRequest(base64Data, fileName, mimeType, fileSize)
        return when (val result = safeApiCall { api.uploadMedia(request) }) {
            is ApiResult.Success -> Result.success(result.data)
            is ApiResult.Error -> Result.failure(Exception(result.message))
        }
    }
}
