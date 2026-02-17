package app.desperse.data.dto.request

import kotlinx.serialization.Serializable

/**
 * Request to initialize auth with backend after Privy login.
 * The backend uses this to sync the Privy user with the backend database.
 */
@Serializable
data class InitAuthRequest(
    val walletAddress: String,
    val email: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class CollectRequest(
    val walletAddress: String? = null
)

@Serializable
data class BuyEditionRequest(
    val postId: String,
    val walletAddress: String? = null
)

@Serializable
data class SubmitSignedTxRequest(
    val purchaseId: String,
    val txSignature: String  // Base58 transaction signature after broadcast
)

@Serializable
data class CreateCommentRequest(
    val content: String
)

@Serializable
data class CreatePostRequest(
    val mediaUrl: String,
    val coverUrl: String? = null,
    val caption: String? = null,
    val categories: List<String>? = null,
    val type: String = "post",
    val assets: List<CreateAssetRequest>? = null,
    val price: Long? = null,
    val currency: String? = null,
    val maxSupply: Int? = null,
    val nftName: String? = null,
    val nftSymbol: String? = null,
    val nftDescription: String? = null,
    val sellerFeeBasisPoints: Int? = null,
    val isMutable: Boolean = true,
    val protectDownload: Boolean = false,
    val mediaMimeType: String? = null,
    val mediaFileSize: Long? = null
)

@Serializable
data class CreateAssetRequest(
    val url: String,
    val mediaType: String,
    val fileName: String,
    val mimeType: String? = null,
    val fileSize: Long? = null,
    val sortOrder: Int
)

@Serializable
data class UpdatePostRequest(
    val caption: String? = null,
    val categories: List<String>? = null,
    val nftName: String? = null,
    val nftSymbol: String? = null,
    val nftDescription: String? = null,
    val sellerFeeBasisPoints: Int? = null,
    val isMutable: Boolean? = null,
    val price: Long? = null,
    val currency: String? = null,
    val maxSupply: Int? = null
)

@Serializable
data class UploadTokenRequest(
    val pathname: String,
    val contentType: String,
    val fileSize: Long? = null
)

@Serializable
data class MarkNotificationsReadRequest(
    val notificationIds: List<String>
)

@Serializable
data class UpdatePreferencesRequest(
    val theme: String? = null,
    val explorer: String? = null,
    val notifications: NotificationPreferencesUpdate? = null,
    val messaging: MessagingPreferencesUpdate? = null
)

@Serializable
data class NotificationPreferencesUpdate(
    val follows: Boolean? = null,
    val likes: Boolean? = null,
    val comments: Boolean? = null,
    val collects: Boolean? = null,
    val purchases: Boolean? = null,
    val mentions: Boolean? = null,
    val messages: Boolean? = null
)

@Serializable
data class MessagingPreferencesUpdate(
    val dmEnabled: Boolean? = null,
    val allowBuyers: Boolean? = null,
    val allowCollectors: Boolean? = null,
    val collectorMinCount: Int? = null,
    val allowTippers: Boolean? = null,
    val tipMinAmount: Int? = null
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val usernameSlug: String? = null,
    val website: String? = null,
    val avatarUrl: String? = null,
    val headerUrl: String? = null
)

@Serializable
data class CreateReportRequest(
    val contentType: String, // "post", "comment", or "dm_thread"
    val contentId: String,
    val reasons: List<String>,
    val details: String? = null
)

@Serializable
data class CreateFeedbackRequest(
    val rating: Int? = null,      // 1-5, optional
    val message: String? = null,  // max 1000 chars, optional
    val imageUrl: String? = null, // uploaded screenshot URL, optional
    val pageUrl: String? = null,  // current screen name
    val appVersion: String? = null,
    val userAgent: String? = null
)

@Serializable
data class UploadImageRequest(
    val fileData: String,  // base64-encoded image data
    val fileName: String,
    val mimeType: String,
    val fileSize: Int
)

@Serializable
data class MediaUploadRequest(
    val fileData: String,  // base64-encoded file data
    val fileName: String,
    val mimeType: String,
    val fileSize: Long
)

// === Tips ===

@Serializable
data class PrepareTipRequest(
    val toUserId: String,
    val amount: Double,
    val context: String,    // "profile" or "message_unlock"
    val walletAddress: String? = null
)

@Serializable
data class ConfirmTipRequest(
    val tipId: String,
    val txSignature: String
)

// === Download Auth ===

@Serializable
data class DownloadNonceRequest(
    val assetId: String
)

@Serializable
data class DownloadVerifyRequest(
    val assetId: String,
    val signature: String,
    val message: String
)

// === Push Notifications ===

@Serializable
data class RegisterPushTokenRequest(
    val token: String,
    val platform: String = "android"
)

@Serializable
data class UnregisterPushTokenRequest(
    val token: String
)
