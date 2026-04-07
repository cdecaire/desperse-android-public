package app.desperse.ui.screens.create.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.desperse.data.upload.UploadState
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.screens.create.UploadedMediaItem
import app.desperse.ui.theme.DesperseSpacing
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Horizontal thumbnail strip for selected media in the details screen.
 * Shows ~1.5 items visible to hint at swiping. Tap to preview full-size.
 */
@Composable
fun MediaThumbnailStrip(
    mediaItems: List<UploadedMediaItem>,
    coverMedia: UploadedMediaItem? = null,
    modifier: Modifier = Modifier
) {
    var previewItem by remember { mutableStateOf<UploadedMediaItem?>(null) }

    // Each item is ~70% of screen width so you see 1 + half of next
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val itemWidth = screenWidth * 0.7f
    val itemHeight = itemWidth * (5f / 4f) // 4:5 ratio

    // For audio/3D posts with a cover, show the cover instead of the non-previewable media
    val displayItems = remember(mediaItems, coverMedia) {
        val hasNonPreviewable = mediaItems.any { it.mediaType in setOf("audio", "3d") }
        if (hasNonPreviewable && coverMedia != null) {
            listOf(coverMedia) + mediaItems.filter { it.mediaType !in setOf("audio", "3d") }
        } else {
            mediaItems
        }
    }

    val isSingle = displayItems.size == 1

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = DesperseSpacing.lg),
        horizontalArrangement = if (isSingle) Arrangement.Center else Arrangement.spacedBy(DesperseSpacing.sm)
    ) {
        items(
            items = displayItems,
            key = { it.id }
        ) { item ->
            MediaThumbnailItem(
                item = item,
                width = itemWidth,
                height = itemHeight,
                onClick = { previewItem = item }
            )
        }
    }

    // Full-screen preview dialog
    previewItem?.let { item ->
        MediaPreviewDialog(
            item = item,
            onDismiss = { previewItem = null }
        )
    }
}

@Composable
private fun MediaThumbnailItem(
    item: UploadedMediaItem,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageUri = item.url ?: item.localUri

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (item.mediaType in setOf("audio", "3d")) {
            // Non-previewable: show icon placeholder
            FaIcon(
                icon = if (item.mediaType == "audio") FaIcons.Music else FaIcons.Cube,
                size = 32.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                style = FaIconStyle.Regular
            )
        } else if (imageUri != null) {
            AsyncImage(
                model = remember(imageUri) {
                    ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .build()
                },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low
            )
        }

        // Upload progress overlay
        when (val state = item.uploadState) {
            is UploadState.Uploading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                }
            }
            is UploadState.Failed -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    FaIcon(
                        icon = FaIcons.CircleExclamation,
                        size = 24.dp,
                        tint = Color.Red,
                        style = FaIconStyle.Solid
                    )
                }
            }
            else -> {}
        }

        // Video indicator
        if (item.mediaType == "video" && item.uploadState is UploadState.Success) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                FaIcon(
                    icon = FaIcons.Play,
                    size = 10.dp,
                    tint = Color.White,
                    style = FaIconStyle.Solid
                )
            }
        }
    }
}

@Composable
private fun MediaPreviewDialog(
    item: UploadedMediaItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageUri = item.url ?: item.localUri

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = remember(imageUri) {
                        ImageRequest.Builder(context)
                            .data(imageUri)
                            .build()
                    },
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
