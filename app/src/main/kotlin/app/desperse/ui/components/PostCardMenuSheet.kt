package app.desperse.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import app.desperse.core.util.openInAppBrowser
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
    fun toastManager(): ToastManager
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
    // Skip all setup work when sheet is closed — this composable is created
    // for every post card in the feed, so avoid unnecessary allocations.
    if (!isOpen) return

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
    val toastManager = remember { entryPoint.toastManager() }
    val postUrl = remember(post.id) { "https://desperse.com/post/${post.id}" }
    val explorerOption by appPreferences.explorer.collectAsState(initial = ExplorerOption.ORB)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Detect media type from URL to determine if downloadable
    val mediaType = remember(post.mediaUrl) { detectMediaType(post.mediaUrl) }
    val isMediaDownloadable = mediaType == MediaType.DOCUMENT || mediaType == MediaType.MODEL_3D
    val fileTypeLabel = remember(post.mediaUrl) { getFileTypeLabel(post.mediaUrl) }

    // Downloadable assets (non-previewable: audio, documents, 3D)
    val downloadableAssets = post.downloadableAssets
    val hasDownloadableAssets = !downloadableAssets.isNullOrEmpty()
    // Has any kind of download: downloadableAssets or media type detection (document/3D)
    val hasDownload = hasDownloadableAssets || isMediaDownloadable
    // NFTs require collection/purchase for download access
    val isNftPost = post.type == "collectible" || post.type == "edition"
    val canDownload = !isNftPost || hasDownloadAccess

    // View on explorer is available when there's an NFT mint address
    // Use collectibleAssetId for collectibles, masterMint for editions
    val explorerAddress = when (post.type) {
        "collectible" -> post.collectibleAssetId
        "edition" -> post.masterMint
        else -> null
    }
    val hasExplorerLink = isNftPost && !explorerAddress.isNullOrBlank()

    DesperseBottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        sheetState = sheetState
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = DesperseSpacing.xxl)
            ) {
                // Go to post
                if (!hideGoToPost) {
                    SheetMenuItem(
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
                    SheetMenuItem(
                        icon = FaIcons.Edit,
                        label = "Edit post",
                        onClick = {
                            onDismiss()
                            onEditPost()
                        }
                    )
                }

                // Share
                SheetMenuItem(
                    icon = FaIcons.Share,
                    label = "Share",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, postUrl)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                        onDismiss()
                    }
                )

                // Copy link
                SheetMenuItem(
                    icon = FaIcons.Link,
                    label = "Copy link",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Post link", postUrl))
                        toastManager.showSuccess("Link copied to clipboard")
                        onDismiss()
                    }
                )

                // Download options
                if (hasDownloadableAssets) {
                    // Show each downloadable asset separately
                    downloadableAssets!!.forEach { asset ->
                        val assetLabel = getFileTypeLabelFromMime(asset.mimeType, asset.url)
                        SheetMenuItem(
                            icon = FaIcons.Download,
                            label = "Download $assetLabel",
                            onClick = {
                                if (!canDownload) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Collect this to download",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else if (isNftPost) {
                                    coroutineScope.launch {
                                        downloadManager.downloadGatedAsset(context, asset.id)
                                    }
                                } else {
                                    downloadManager.downloadFreeAsset(context, asset.id)
                                }
                                onDismiss()
                            },
                            trailing = if (!canDownload) {{ FaIcon(FaIcons.Lock, size = 12.dp, style = FaIconStyle.Solid, tint = MaterialTheme.colorScheme.onSurfaceVariant) }} else null,
                            tint = if (!canDownload) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else if (hasDownload) {
                    // Fallback: use assetId or show based on media type detection
                    SheetMenuItem(
                        icon = FaIcons.Download,
                        label = "Download ${if (isMediaDownloadable) fileTypeLabel else ""}".trim(),
                        onClick = {
                            if (!canDownload) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Collect this to download",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val downloadAssetId = post.assetId
                                if (downloadAssetId != null) {
                                    if (isNftPost) {
                                        coroutineScope.launch {
                                            downloadManager.downloadGatedAsset(context, downloadAssetId)
                                        }
                                    } else {
                                        downloadManager.downloadFreeAsset(context, downloadAssetId)
                                    }
                                }
                            }
                            onDismiss()
                        },
                        trailing = if (!canDownload) {{ FaIcon(FaIcons.Lock, size = 12.dp, style = FaIconStyle.Solid, tint = MaterialTheme.colorScheme.onSurfaceVariant) }} else null,
                        tint = if (!canDownload) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }

                // View on explorer (if NFT with valid address)
                if (hasExplorerLink) {
                    SheetMenuItem(
                        icon = FaIcons.Cube,
                        label = "View on explorer",
                        onClick = {
                            val explorerUrl = explorerOption.getExplorerUrl(explorerAddress!!)
                            context.openInAppBrowser(explorerUrl)
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
                    SheetMenuItem(
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
                    SheetMenuItem(
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

