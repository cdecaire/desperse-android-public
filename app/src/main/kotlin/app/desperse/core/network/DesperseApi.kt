package app.desperse.core.network

import app.desperse.core.wallet.AddWalletResponse
import app.desperse.core.wallet.UserWalletsResponse
import app.desperse.data.dto.AddWalletRequest
import app.desperse.data.dto.UpdateWalletLabelRequest
import app.desperse.data.dto.request.*
import app.desperse.data.dto.response.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Desperse API interface - all paths use /api/v1/
 */
interface DesperseApi {

    // === Health & Version ===
    @GET("api/v1/health")
    suspend fun health(): Response<ApiEnvelope<HealthResult>>

    @GET("api/v1/version")
    suspend fun version(): Response<ApiEnvelope<VersionResult>>

    // === Auth (Tier 1) ===
    @POST("api/v1/auth/init")
    suspend fun initAuth(
        @Body request: InitAuthRequest
    ): Response<ApiEnvelope<AuthResult>>

    @GET("api/v1/users/me")
    suspend fun getCurrentUser(): Response<ApiEnvelope<UserResult>>

    @GET("api/v1/users/{slug}")
    suspend fun getUserProfile(
        @Path("slug") slug: String
    ): Response<ApiEnvelope<ProfileResult>>

    @GET("api/v1/users/{slug}/posts")
    suspend fun getUserPosts(
        @Path("slug") slug: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<ApiEnvelope<UserPostsResult>>

    @GET("api/v1/users/{slug}/collected")
    suspend fun getUserCollected(
        @Path("slug") slug: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<ApiEnvelope<UserPostsResult>>

    @GET("api/v1/users/{slug}/for-sale")
    suspend fun getUserForSale(
        @Path("slug") slug: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<ApiEnvelope<UserPostsResult>>

    // === Editions - THE GATE (Tier 2) ===
    @POST("api/v1/editions/buy")
    suspend fun buyEdition(
        @Body request: BuyEditionRequest,
        @Header("Idempotency-Key") idempotencyKey: String
    ): Response<ApiEnvelope<BuyEditionResult>>

    @POST("api/v1/editions/signature")
    suspend fun submitSignedTransaction(
        @Body request: SubmitSignedTxRequest
    ): Response<ApiEnvelope<SubmitResult>>

    @GET("api/v1/editions/purchase/{id}/status")
    suspend fun checkPurchaseStatus(
        @Path("id") purchaseId: String
    ): Response<ApiEnvelope<PurchaseStatusResult>>

    // === Posts (Tier 3) ===
    @GET("api/v1/posts")
    suspend fun getPosts(
        @Query("tab") tab: String = "for-you",
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<ApiEnvelope<PostsResult>>

    @GET("api/v1/posts/{id}")
    suspend fun getPost(
        @Path("id") postId: String
    ): Response<ApiEnvelope<PostResult>>

    // === Likes (Tier 4) ===
    @POST("api/v1/posts/{id}/like")
    suspend fun likePost(
        @Path("id") postId: String
    ): Response<ApiEnvelope<LikeResult>>

    @DELETE("api/v1/posts/{id}/like")
    suspend fun unlikePost(
        @Path("id") postId: String
    ): Response<ApiEnvelope<LikeResult>>

    // === Collectibles (Tier 4) ===
    @POST("api/v1/posts/{id}/collect")
    suspend fun collectPost(
        @Path("id") postId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CollectRequest
    ): Response<ApiEnvelope<CollectResult>>

    @GET("api/v1/collections/{id}/status")
    suspend fun checkCollectionStatus(
        @Path("id") collectionId: String
    ): Response<ApiEnvelope<CollectionStatusResult>>

    // === Follow (Tier 4) ===
    @POST("api/v1/users/{id}/follow")
    suspend fun followUser(
        @Path("id") userId: String
    ): Response<ApiEnvelope<FollowResult>>

    @DELETE("api/v1/users/{id}/follow")
    suspend fun unfollowUser(
        @Path("id") userId: String
    ): Response<ApiEnvelope<FollowResult>>

    @GET("api/v1/users/{slug}/followers")
    suspend fun getUserFollowers(
        @Path("slug") slug: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<ApiEnvelope<FollowListResult>>

    @GET("api/v1/users/{slug}/following")
    suspend fun getUserFollowing(
        @Path("slug") slug: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<ApiEnvelope<FollowListResult>>

    @GET("api/v1/users/{slug}/collectors")
    suspend fun getUserCollectors(
        @Path("slug") slug: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<ApiEnvelope<FollowListResult>>

    // === Activity (own user only) ===
    @GET("api/v1/users/me/activity")
    suspend fun getUserActivity(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<ApiEnvelope<ActivityResult>>

    // === Comments (Tier 4) ===
    @GET("api/v1/posts/{id}/comments")
    suspend fun getComments(
        @Path("id") postId: String,
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null
    ): Response<ApiEnvelope<CommentsResult>>

    @POST("api/v1/posts/{id}/comments")
    suspend fun createComment(
        @Path("id") postId: String,
        @Body request: CreateCommentRequest
    ): Response<ApiEnvelope<CommentResult>>

    @DELETE("api/v1/posts/{postId}/comments/{commentId}")
    suspend fun deleteComment(
        @Path("postId") postId: String,
        @Path("commentId") commentId: String
    ): Response<ApiEnvelope<DeleteCommentResult>>

    // === Explore ===
    @GET("api/v1/explore/suggested-creators")
    suspend fun getSuggestedCreators(
        @Query("limit") limit: Int = 8
    ): Response<ApiEnvelope<SuggestedCreatorsResult>>

    @GET("api/v1/explore/trending")
    suspend fun getTrendingPosts(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20
    ): Response<ApiEnvelope<TrendingPostsResult>>

    // === Search ===
    @GET("api/v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "all",
        @Query("limit") limit: Int = 20
    ): Response<ApiEnvelope<SearchResult>>

    // === Wallet ===
    @GET("api/v1/wallet/overview")
    suspend fun getWalletOverview(): Response<ApiEnvelope<WalletOverviewResult>>

    // Wallet preferences
    @GET("api/v1/wallet/wallets")
    suspend fun getUserWallets(): Response<ApiEnvelope<UserWalletsResponse>>

    @POST("api/v1/wallet/add")
    suspend fun addWallet(@Body request: AddWalletRequest): Response<ApiEnvelope<AddWalletResponse>>

    @HTTP(method = "DELETE", path = "api/v1/wallet/{id}", hasBody = false)
    suspend fun removeWallet(@Path("id") walletId: String): Response<ApiEnvelope<Unit>>

    @PUT("api/v1/wallet/{id}/default")
    suspend fun setDefaultWallet(@Path("id") walletId: String): Response<ApiEnvelope<Unit>>

    @PUT("api/v1/wallet/label")
    suspend fun updateWalletLabel(@Body request: UpdateWalletLabelRequest): Response<ApiEnvelope<Unit>>

    // === Notifications ===
    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<ApiEnvelope<NotificationsResult>>

    @POST("api/v1/notifications/read")
    suspend fun markNotificationsRead(
        @Body request: MarkNotificationsReadRequest
    ): Response<ApiEnvelope<MarkNotificationsReadResult>>

    @POST("api/v1/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ApiEnvelope<Unit>>

    @DELETE("api/v1/notifications")
    suspend fun clearAllNotifications(): Response<ApiEnvelope<Unit>>

    @GET("api/v1/notifications/counters")
    suspend fun getNotificationCounters(
        @Query("forYouLastSeen") forYouLastSeen: String? = null,
        @Query("followingLastSeen") followingLastSeen: String? = null
    ): Response<ApiEnvelope<NotificationCountersResult>>

    // === User Preferences ===
    @GET("api/v1/users/me/preferences")
    suspend fun getPreferences(): Response<ApiEnvelope<PreferencesResult>>

    @PATCH("api/v1/users/me/preferences")
    suspend fun updatePreferences(
        @Body request: UpdatePreferencesRequest
    ): Response<ApiEnvelope<PreferencesResult>>

    // === Profile Update ===
    @PATCH("api/v1/users/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<ApiEnvelope<UpdateProfileResult>>

    // === Mention Search ===
    @GET("api/v1/users/mention-search")
    suspend fun searchMentionUsers(
        @Query("query") query: String? = null,
        @Query("limit") limit: Int = 8
    ): Response<ApiEnvelope<MentionSearchResult>>

    // === Reports ===
    @POST("api/v1/reports")
    suspend fun createReport(
        @Body request: CreateReportRequest
    ): Response<ApiEnvelope<ReportResult>>

    // === Feedback ===
    @POST("api/v1/feedback")
    suspend fun createFeedback(
        @Body request: CreateFeedbackRequest
    ): Response<ApiEnvelope<FeedbackResult>>

    // === Image Upload ===
    @POST("api/v1/users/me/avatar")
    suspend fun uploadAvatar(
        @Body request: UploadImageRequest
    ): Response<ApiEnvelope<UploadImageResult>>

    @POST("api/v1/users/me/header")
    suspend fun uploadHeader(
        @Body request: UploadImageRequest
    ): Response<ApiEnvelope<UploadImageResult>>

    // === Post CRUD ===
    @POST("api/v1/posts")
    suspend fun createPost(
        @Body request: CreatePostRequest
    ): Response<ApiEnvelope<CreatePostResult>>

    @PATCH("api/v1/posts/{id}")
    suspend fun updatePost(
        @Path("id") postId: String,
        @Body request: UpdatePostRequest
    ): Response<ApiEnvelope<UpdatePostResult>>

    @DELETE("api/v1/posts/{id}")
    suspend fun deletePost(
        @Path("id") postId: String
    ): Response<ApiEnvelope<DeletePostResult>>

    @GET("api/v1/posts/{id}/edit-state")
    suspend fun getEditState(
        @Path("id") postId: String
    ): Response<ApiEnvelope<EditStateResult>>

    // === Media Upload Token ===
    @POST("api/v1/media/upload-token")
    suspend fun getUploadToken(
        @Body request: UploadTokenRequest
    ): Response<ApiEnvelope<UploadTokenResult>>

    // === Media Upload ===
    @POST("api/v1/media/upload")
    suspend fun uploadMedia(
        @Body request: MediaUploadRequest
    ): Response<ApiEnvelope<MediaUploadResult>>

    // === Messages ===

    @GET("api/v1/messages/threads")
    suspend fun getThreads(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<ApiEnvelope<ThreadListResponse>>

    @POST("api/v1/messages/threads")
    suspend fun getOrCreateThread(
        @Body request: CreateThreadRequest
    ): Response<ApiEnvelope<CreateThreadResponse>>

    @GET("api/v1/messages/threads/{threadId}/messages")
    suspend fun getMessages(
        @Path("threadId") threadId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<ApiEnvelope<MessagesListResponse>>

    @POST("api/v1/messages/threads/{threadId}/messages")
    suspend fun sendMessage(
        @Path("threadId") threadId: String,
        @Body request: SendMessageRequest
    ): Response<ApiEnvelope<SendMessageResponse>>

    @POST("api/v1/messages/threads/{threadId}/read")
    suspend fun markThreadRead(
        @Path("threadId") threadId: String
    ): Response<ApiEnvelope<MarkReadResponse>>

    @POST("api/v1/messages/threads/{threadId}/block")
    suspend fun blockInThread(
        @Path("threadId") threadId: String,
        @Body request: BlockRequest
    ): Response<ApiEnvelope<BlockResponse>>

    @POST("api/v1/messages/threads/{threadId}/archive")
    suspend fun archiveThread(
        @Path("threadId") threadId: String,
        @Body request: ArchiveRequest
    ): Response<ApiEnvelope<ArchiveResponse>>

    @DELETE("api/v1/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("messageId") messageId: String
    ): Response<ApiEnvelope<Unit>>

    @GET("api/v1/messages/eligibility")
    suspend fun checkDmEligibility(
        @Query("creatorId") creatorId: String
    ): Response<ApiEnvelope<DmEligibilityResponse>>

    @GET("api/v1/messages/preferences")
    suspend fun getDmPreferences(): Response<ApiEnvelope<DmPreferencesResponse>>

    @PUT("api/v1/messages/preferences")
    suspend fun updateDmPreferences(
        @Body request: UpdateDmPreferencesRequest
    ): Response<ApiEnvelope<DmPreferencesResponse>>

    // === Ably Realtime Token ===

    @POST("api/v1/ably/token")
    suspend fun getAblyToken(): Response<ApiEnvelope<AblyTokenResponse>>

    // === Tips ===

    @POST("api/v1/tips/prepare")
    suspend fun prepareTip(
        @Body request: PrepareTipRequest
    ): Response<ApiEnvelope<PrepareTipResult>>

    @POST("api/v1/tips/confirm")
    suspend fun confirmTip(
        @Body request: ConfirmTipRequest
    ): Response<ApiEnvelope<ConfirmTipResult>>

    @GET("api/v1/tips/stats")
    suspend fun getTipStats(
        @Query("userId") userId: String
    ): Response<ApiEnvelope<TipStatsResult>>

    // === Download Auth ===

    @POST("api/v1/downloads/nonce")
    suspend fun getDownloadNonce(@Body request: DownloadNonceRequest): Response<ApiEnvelope<DownloadNonceResult>>

    @POST("api/v1/downloads/verify")
    suspend fun verifyDownload(@Body request: DownloadVerifyRequest): Response<ApiEnvelope<DownloadVerifyResult>>

    // === Push Notifications ===

    @POST("api/v1/users/me/push-token")
    suspend fun registerPushToken(
        @Body request: RegisterPushTokenRequest
    ): Response<ApiEnvelope<Unit>>

    @HTTP(method = "DELETE", path = "api/v1/users/me/push-token", hasBody = true)
    suspend fun unregisterPushToken(
        @Body request: UnregisterPushTokenRequest
    ): Response<ApiEnvelope<Unit>>
}
