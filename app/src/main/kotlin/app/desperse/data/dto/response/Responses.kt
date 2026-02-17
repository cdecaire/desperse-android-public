package app.desperse.data.dto.response

import app.desperse.data.model.Post
import app.desperse.data.model.User
import kotlinx.serialization.Serializable

@Serializable
data class HealthResult(
    val status: String,
    val api: String
)

@Serializable
data class VersionResult(
    val api: String,
    val build: String,
    val env: String,
    val minAndroidVersion: String? = null,
    val currentAndroidVersion: String? = null
)

@Serializable
data class AuthResult(
    val user: User? = null,
    val isNewUser: Boolean? = null,
    val token: String? = null
)

@Serializable
data class UserResult(
    val user: User?
)

/**
 * Profile endpoint response - includes stats and follow info
 */
@Serializable
data class ProfileResult(
    val user: ProfileUser,
    val stats: ProfileStats,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val collectorsCount: Int = 0,
    val isFollowing: Boolean = false
)

@Serializable
data class ProfileUser(
    val id: String,
    val slug: String,
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val headerBgUrl: String? = null,
    val link: String? = null,
    val createdAt: String? = null
)

@Serializable
data class ProfileStats(
    val posts: Int = 0,
    val collected: Int = 0,
    val forSale: Int = 0
)

/**
 * User posts endpoint response (paginated)
 */
@Serializable
data class UserPostsResult(
    val posts: List<Post>
)

@Serializable
data class PostsResult(
    val posts: List<Post>
)

@Serializable
data class PostResult(
    val post: Post
)

@Serializable
data class BuyEditionResult(
    val purchaseId: String,
    val unsignedTxBase64: String,
    val priceDisplay: String,
    val expiresAt: String
)

@Serializable
data class SubmitResult(
    val status: String,
    val txSignature: String? = null
)

@Serializable
data class PurchaseStatusResult(
    val status: String,
    val txSignature: String? = null,
    val nftMint: String? = null,
    val error: String? = null
)

@Serializable
data class LikeResult(
    val isLiked: Boolean
)

@Serializable
data class CollectResult(
    val collectionId: String? = null,
    val txSignature: String? = null,
    val assetId: String? = null,
    val status: String? = null,
    val error: String? = null,
    val message: String? = null
)

@Serializable
data class CollectionStatusResult(
    val status: String,
    val txSignature: String? = null,
    val nftMint: String? = null,
    val error: String? = null
)

@Serializable
data class FollowResult(
    val isFollowing: Boolean
)

@Serializable
data class CommentsResult(
    val comments: List<Comment>
)

@Serializable
data class CommentResult(
    val comment: Comment
)

@Serializable
data class Comment(
    val id: String,
    val content: String,
    val createdAt: String,
    val user: User
)

@Serializable
data class DeleteCommentResult(
    val deleted: Boolean
)

/**
 * Followers/Following/Collectors list response
 */
@Serializable
data class FollowListResult(
    val users: List<FollowUser>
)

@Serializable
data class FollowUser(
    val id: String,
    val slug: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val isFollowing: Boolean = false
)

/**
 * Activity feed response
 */
@Serializable
data class ActivityResult(
    val activities: List<ActivityItem>
)

@Serializable
data class ActivityItem(
    val id: String,
    val type: String, // "post", "like", "commented", "collected", "bought", "tipped"
    val timestamp: String,
    val post: ActivityPost? = null,
    val tip: ActivityTip? = null
)

@Serializable
data class ActivityPost(
    val id: String,
    val type: String,
    val caption: String? = null,
    val mediaUrl: String? = null,
    val coverUrl: String? = null,
    val user: ActivityPostUser
)

@Serializable
data class ActivityPostUser(
    val id: String,
    val slug: String,
    val displayName: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class ActivityTip(
    val amount: Double,
    val token: String,
    val recipient: ActivityPostUser
)

/**
 * Explore - Suggested Creators response
 */
@Serializable
data class SuggestedCreatorsResult(
    val creators: List<SuggestedCreator>
)

@Serializable
data class SuggestedCreator(
    val id: String,
    val usernameSlug: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val followerCount: Int = 0,
    val isNew: Boolean = false
)

/**
 * Explore - Trending posts response
 */
@Serializable
data class TrendingPostsResult(
    val posts: List<Post>,
    val hasMore: Boolean = false,
    val nextOffset: Int? = null,
    val isFallback: Boolean = false,
    val sectionTitle: String = "Trending"
)

/**
 * Search response
 */
@Serializable
data class SearchResult(
    val users: List<SearchUser>,
    val posts: List<Post>,
    val query: String
)

@Serializable
data class SearchUser(
    val id: String,
    val usernameSlug: String,
    val displayName: String? = null,
    val avatarUrl: String? = null
)

// ============================================================
// Wallet DTOs
// ============================================================

/**
 * Wallet overview response
 */
@Serializable
data class WalletOverviewResult(
    val totalUsd: Double = 0.0,
    val solPriceUsd: Double = 0.0,
    val solChangePct24h: Double = 0.0,
    val wallets: List<WalletBalance> = emptyList(),
    val tokens: List<TokenBalance> = emptyList(),
    val activity: List<WalletActivityItem> = emptyList(),
    val nfts: List<NFTAsset> = emptyList()
)

@Serializable
data class WalletBalance(
    val address: String,
    val walletClientType: String? = null,
    val sol: Double = 0.0,
    val usdc: Double = 0.0,
    val usdValue: Double = 0.0
)

@Serializable
data class TokenBalance(
    val mint: String,
    val symbol: String,
    val name: String,
    val iconUrl: String? = null,
    val balance: Double = 0.0,
    val decimals: Int = 0,
    val priceUsd: Double? = null,
    val totalValueUsd: Double? = null,
    val changePct24h: Double? = null,
    val isAppToken: Boolean = false
)

@Serializable
data class WalletActivityItem(
    val id: String,
    val signature: String? = null,
    val token: String? = null, // "SOL" | "USDC" | null
    val amount: Double? = null,
    val direction: String? = null, // "in" | "out" | null
    val timestamp: Long,
    val type: String, // "edition_sale" | "edition_purchase" | "collection" | "transfer_in" | "transfer_out"
    val context: WalletActivityContext
)

@Serializable
data class WalletActivityContext(
    val type: String,
    val post: WalletActivityPost? = null,
    val counterparty: WalletActivityUser? = null,
    val creator: WalletActivityUser? = null
)

@Serializable
data class WalletActivityPost(
    val id: String,
    val caption: String? = null,
    val coverUrl: String? = null,
    val mediaUrl: String
)

@Serializable
data class WalletActivityUser(
    val id: String,
    val displayName: String? = null,
    val usernameSlug: String,
    val avatarUrl: String? = null
)

@Serializable
data class NFTAsset(
    val mint: String,
    val name: String? = null,
    val imageUri: String? = null,
    val collectionName: String? = null,
    val collectionAddress: String? = null,
    val compressed: Boolean = false
)

// ============================================================
// Notification DTOs
// ============================================================

/**
 * Notifications list response
 */
@Serializable
data class NotificationsResult(
    val notifications: List<NotificationItem>
)

/**
 * A single notification item
 */
@Serializable
data class NotificationItem(
    val id: String,
    val type: String, // "follow" | "like" | "comment" | "collect" | "purchase" | "mention"
    val referenceType: String? = null, // "post" | "comment" | null
    val referenceId: String? = null,
    val isRead: Boolean = false,
    val createdAt: String,
    val actor: NotificationActor,
    val reference: NotificationReference? = null
)

/**
 * The user who triggered the notification
 */
@Serializable
data class NotificationActor(
    val id: String,
    val displayName: String? = null,
    val usernameSlug: String,
    val avatarUrl: String? = null
)

/**
 * Reference to the post or comment associated with the notification
 */
@Serializable
data class NotificationReference(
    val mediaUrl: String? = null,
    val coverUrl: String? = null,
    val caption: String? = null,
    val content: String? = null, // For comment notifications
    val postId: String? = null   // For comment notifications, the parent post ID
)

/**
 * Response for marking notifications as read
 */
@Serializable
data class MarkNotificationsReadResult(
    val markedCount: Int = 0
)

/**
 * Notification counters for badges/indicators
 */
@Serializable
data class NotificationCountersResult(
    val unreadNotifications: Int = 0,
    val forYou: FeedCounters? = null,
    val following: FeedCounters? = null
)

@Serializable
data class FeedCounters(
    val newPostsCount: Int = 0,
    val creators: List<CounterCreator> = emptyList()
)

@Serializable
data class CounterCreator(
    val id: String,
    val avatarUrl: String? = null,
    val displayName: String? = null,
    val slug: String
)

// === User Preferences ===

@Serializable
data class PreferencesResult(
    val preferences: UserPreferences
)

@Serializable
data class UserPreferences(
    val theme: String? = "system",
    val explorer: String? = "orb",
    val notifications: NotificationPreferences? = null,
    val messaging: MessagingPreferencesDto? = null
)

@Serializable
data class NotificationPreferences(
    val follows: Boolean = true,
    val likes: Boolean = true,
    val comments: Boolean = true,
    val collects: Boolean = true,
    val purchases: Boolean = true,
    val mentions: Boolean = true,
    val messages: Boolean = true
)

@Serializable
data class MessagingPreferencesDto(
    val dmEnabled: Boolean = true,
    val allowBuyers: Boolean = true,
    val allowCollectors: Boolean = true,
    val collectorMinCount: Int = 3,
    val allowTippers: Boolean = true,
    val tipMinAmount: Int = 50
)

// === Profile Update ===

@Serializable
data class UpdateProfileResult(
    val user: UpdatedUser
)

@Serializable
data class UpdatedUser(
    val id: String,
    val slug: String,
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val headerBgUrl: String? = null,
    val link: String? = null,
    val walletAddress: String? = null
)

// === Mention Search ===

@Serializable
data class MentionSearchResult(
    val users: List<MentionUser>
)

@Serializable
data class MentionUser(
    val id: String,
    val usernameSlug: String,
    val displayName: String? = null,
    val avatarUrl: String? = null
)

// === Reports ===

@Serializable
data class ReportResult(
    val reportId: String
)

// === Feedback ===

@Serializable
data class FeedbackResult(
    val id: String
)

// === Image Upload ===

@Serializable
data class UploadImageResult(
    val url: String
)

// === Upload Token ===

@Serializable
data class UploadTokenResult(
    val clientToken: String
)

// === Media Upload ===

@Serializable
data class MediaUploadResult(
    val url: String,
    val pathname: String,
    val mediaType: String
)

// === Post Create/Update/Delete ===

@Serializable
data class CreatePostResult(
    val post: Post
)

@Serializable
data class UpdatePostResult(
    val post: Post
)

@Serializable
data class DeletePostResult(
    val warning: String? = null
)

// === Edit State ===

@Serializable
data class EditStateResult(
    val hasConfirmedCollects: Boolean = false,
    val hasConfirmedPurchases: Boolean = false,
    val isMinted: Boolean = false,
    val mintedAt: String? = null,
    val mintedIsMutable: Boolean = true,
    val areNftFieldsLocked: Boolean = false,
    val canUpdateOnChain: Boolean = false,
    val onchainSyncStatus: String? = null,
    val lastOnchainSyncAt: String? = null
)

// === Tips ===

@Serializable
data class PrepareTipResult(
    val tipId: String,
    val transaction: String,         // base64 unsigned tx
    val blockhash: String? = null,
    val lastValidBlockHeight: Long? = null
)

@Serializable
data class ConfirmTipResult(
    val status: String
)

@Serializable
data class TipStatsResult(
    val totalReceived: Double = 0.0,
    val tipCount: Int = 0
)

// === Download Auth ===

@Serializable
data class DownloadNonceResult(
    val nonce: String,
    val message: String,
    val expiresAt: String
)

@Serializable
data class DownloadVerifyResult(
    val token: String,
    val expiresAt: Long
)
