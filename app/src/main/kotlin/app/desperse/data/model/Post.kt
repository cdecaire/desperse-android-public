package app.desperse.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Post(
    val id: String,
    val type: String, // "post", "collectible", "edition"
    val caption: String? = null,
    val mediaUrl: String? = null,
    val coverUrl: String? = null,
    val createdAt: String,
    val user: User,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val collectCount: Int = 0,
    val isLiked: Boolean = false,
    val isCollected: Boolean = false,
    // Edition-specific
    val price: Double? = null,
    val currency: String? = null, // "SOL" or "USDC"
    val maxSupply: Int? = null,
    val currentSupply: Int? = null,
    val nftName: String? = null,
    // NFT blockchain addresses (for explorer links)
    val masterMint: String? = null, // Master mint address for editions
    val collectibleAssetId: String? = null, // Asset ID for collectibles (cNFTs)
    // Multi-asset posts (only present if >1 asset)
    val assets: List<PostAsset>? = null,
    // Downloadable assets (non-previewable: audio, documents, 3D)
    val downloadableAssets: List<DownloadableAsset>? = null
)

/**
 * Asset attached to a post (for multi-image carousels)
 */
@Immutable
@Serializable
data class PostAsset(
    val id: String,
    val url: String,
    val mimeType: String,
    val sortOrder: Int
)

/**
 * Downloadable asset (non-previewable: audio, documents, 3D models)
 */
@Immutable
@Serializable
data class DownloadableAsset(
    val id: String,
    val url: String,
    val mimeType: String? = null,
    val fileSize: Long? = null,
    val sortOrder: Int = 0
)

/**
 * State machine for collecting a post (cNFT minting).
 * States: idle → preparing → confirming → success
 *                        ↘ failed ←─────┘
 */
sealed class CollectState {
    data object Idle : CollectState()
    data object Preparing : CollectState()
    data class Confirming(val collectionId: String) : CollectState()
    data object Success : CollectState()
    data class Failed(val error: String, val canRetry: Boolean = true) : CollectState()
}

/**
 * State machine for purchasing an edition (paid NFT).
 * States: idle → preparing → signing → broadcasting → submitting → confirming → success
 *                   ↘───────────────────────→ failed ←────────────────────────┘
 */
sealed class PurchaseState {
    data object Idle : PurchaseState()
    data object Preparing : PurchaseState()          // Getting unsigned tx from server
    data object Signing : PurchaseState()            // User signing with Privy wallet
    data object Broadcasting : PurchaseState()       // Sending signed tx to Solana network
    data object Submitting : PurchaseState()         // Submitting signature to backend
    data class Confirming(val purchaseId: String) : PurchaseState()  // Polling for on-chain confirmation
    data object Success : PurchaseState()
    data class Failed(val error: String, val canRetry: Boolean = true) : PurchaseState()
}
