package app.desperse.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.desperse.core.download.GatedDownloadManager
import app.desperse.core.preferences.ExplorerOption
import app.desperse.data.model.Post
import app.desperse.ui.components.media.MediaType
import app.desperse.ui.components.media.detectMediaType
import app.desperse.ui.components.media.getFileTypeLabel
import app.desperse.ui.components.media.getFileTypeLabelFromMime
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

/**
 * Post Card Menu Bottom Sheet
 *
 * Native-feeling bottom sheet with post actions matching web design.
 * Actions:
 * - Go to post
 * - Copy link
 * - View on explorer (if NFT)
 * - Download (if purchased and has gated download)
 * - Report post
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PostCardMenuEntryPoint {
    fun appPreferences(): app.desperse.core.preferences.AppPreferences
    fun gatedDownloadManager(): GatedDownloadManager
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCardMenuSheet(
    isOpen: Boolean,
    post: Post,
    onDismiss: () -> Unit,
    onGoToPost: () -> Unit,
    onReport: () -> Unit = {},
    onEditPost: () -> Unit = {},
    onDeletePost: () -> Unit = {},
    /** Hide "Go to post" option (useful on post detail pages) */
    hideGoToPost: Boolean = false,
    /** Whether user has access to download (purchased/collected) */
    hasDownloadAccess: Boolean = false,
    /** Whether this is the current user's own post */
    isOwnPost: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PostCardMenuEntryPoint::class.java
        )
    }
    val appPreferences = remember { entryPoint.appPreferences() }
    val downloadManager = remember { entryPoint.gatedDownloadManager() }
    val explorerOption by appPreferences.explorer.collectAsState(initial = ExplorerOption.ORB)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Detect media type from URL to determine if downloadable
    val mediaType = remember(post.mediaUrl) { detectMediaType(post.mediaUrl) }

    // Download option shows for documents (PDF, ZIP, EPUB) or 3D models
    val isDownloadable = mediaType == MediaType.DOCUMENT || mediaType == MediaType.MODEL_3D
    val isLocked = isDownloadable && !hasDownloadAccess
    val fileTypeLabel = remember(post.mediaUrl) { getFileTypeLabel(post.mediaUrl) }

    // Downloadable assets (non-previewable: audio, documents, 3D) from multi-asset posts
    val downloadableAssets = post.downloadableAssets
    val hasDownloadableAssets = !downloadableAssets.isNullOrEmpty()
    // Posts and collectibles always have free downloads; editions require purchase
    val canDownloadAssets = post.type == "post" || post.type == "collectible" || hasDownloadAccess
    // Editions use gated download flow with signature verification
    val isGatedPost = post.type == "edition"

    // View on explorer is available when there's an NFT mint address
    // This is independent of download - regular collectibles/editions show this
    val isNft = post.type == "collectible" || post.type == "edition"
    // Use collectibleAssetId for collectibles, masterMint for editions
    val explorerAddress = when (post.type) {
        "collectible" -> post.collectibleAssetId
        "edition" -> post.masterMint
        else -> null
    }
    val hasExplorerLink = isNft && !explorerAddress.isNullOrBlank()

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = {
                // Custom drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DesperseSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = DesperseSpacing.xxl)
            ) {
                // Go to post
                if (!hideGoToPost) {
                    PostMenuItem(
                        icon = FaIcons.ArrowRight,
                        label = "Go to post",
                        onClick = {
                            onDismiss()
                            onGoToPost()
                        }
                    )
                }

                // Edit post (own posts only)
                if (isOwnPost) {
                    PostMenuItem(
                        icon = FaIcons.Edit,
                        label = "Edit post",
                        onClick = {
                            onDismiss()
                            onEditPost()
                        }
                    )
                }

                // Copy link
                PostMenuItem(
                    icon = FaIcons.Link,
                    label = "Copy link",
                    onClick = {
                        val postUrl = "https://desperse.com/p/${post.id}"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Post link", postUrl))
                        onDismiss()
                        // TODO: Show toast "Link copied to clipboard"
                    }
                )

                // Download (shows for all document posts, locked if no access)
                if (isDownloadable) {
                    PostMenuItem(
                        icon = FaIcons.Download,
                        label = "Download $fileTypeLabel",
                        onClick = {
                            if (isLocked) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Purchase this edition to download",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                // Open download URL in browser
                                post.mediaUrl?.let { url ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            }
                            onDismiss()
                        },
                        trailingIcon = if (isLocked) FaIcons.Lock else null,
                        tint = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Download options for separate downloadable assets (multi-asset posts)
                if (hasDownloadableAssets) {
                    downloadableAssets!!.forEach { asset ->
                        val assetLabel = getFileTypeLabelFromMime(asset.mimeType, asset.url)
                        PostMenuItem(
                            icon = FaIcons.Download,
                            label = "Download $assetLabel",
                            onClick = {
                                if (!canDownloadAssets) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Purchase this edition to download",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else if (isGatedPost) {
                                    // Edition downloads use signature verification flow
                                    coroutineScope.launch {
                                        downloadManager.downloadGatedAsset(context, asset.id)
                                    }
                                } else {
                                    // Posts/collectibles have free downloads via direct URL
                                    downloadManager.downloadFreeAsset(context, asset.id)
                                }
                                onDismiss()
                            },
                            trailingIcon = if (!canDownloadAssets) FaIcons.Lock else null,
                            tint = if (!canDownloadAssets) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // View on explorer (if NFT with valid address)
                if (hasExplorerLink) {
                    PostMenuItem(
                        icon = FaIcons.Cube,
                        label = "View on explorer",
                        onClick = {
                            val explorerUrl = explorerOption.getExplorerUrl(explorerAddress!!)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(explorerUrl))
                            context.startActivity(intent)
                            onDismiss()
                        }
                    )
                }

                // Separator before destructive actions
                HorizontalDivider(
                    modifier = Modifier.padding(
                        horizontal = DesperseSpacing.lg,
                        vertical = DesperseSpacing.xs
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Delete post (own posts that haven't been minted)
                if (isOwnPost) {
                    val isMinted = !post.masterMint.isNullOrBlank() || !post.collectibleAssetId.isNullOrBlank()
                    PostMenuItem(
                        icon = FaIcons.Trash,
                        label = "Delete post",
                        tint = MaterialTheme.colorScheme.error,
                        enabled = !isMinted,
                        onClick = {
                            onDismiss()
                            onDeletePost()
                        }
                    )
                }

                // Report post (other people's posts)
                if (!isOwnPost) {
                    PostMenuItem(
                        icon = FaIcons.Flag,
                        label = "Report post",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            onDismiss()
                            onReport()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PostMenuItem(
    icon: String,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    trailingIcon: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(
                horizontal = DesperseSpacing.lg,
                vertical = DesperseSpacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FaIcon(
            icon = icon,
            size = DesperseSizes.iconMd,
            style = FaIconStyle.Regular,
            tint = if (enabled) tint else tint.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.width(DesperseSpacing.md))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) tint else tint.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )

        if (trailingIcon != null) {
            FaIcon(
                icon = trailingIcon,
                size = 12.dp,
                style = FaIconStyle.Solid,
                tint = if (enabled) tint else tint.copy(alpha = 0.5f)
            )
        }
    }
}
