package app.desperse.ui.screens.create.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.desperse.data.model.MediaConstants
import app.desperse.data.upload.UploadState
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.screens.create.UploadedMediaItem
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.toneDestructive
import app.desperse.ui.theme.toneStandard
import coil.compose.AsyncImage

private const val MAX_ITEMS = 10

@Composable
fun MultiMediaPicker(
    mediaItems: List<UploadedMediaItem>,
    isLocked: Boolean,
    onAddMedia: (Uri) -> Unit,
    onRemoveMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAddMedia(it) }
    }

    val previewableItems = remember(mediaItems) {
        mediaItems.filter { MediaConstants.isPreviewable(it.mediaType) }
    }
    val downloadableItem = remember(mediaItems) {
        mediaItems.firstOrNull { !MediaConstants.isPreviewable(it.mediaType) }
    }
    val canAddMore = mediaItems.size < MAX_ITEMS && !isLocked

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)) {
        // Previewable grid (images/videos)
        if (previewableItems.isEmpty() && downloadableItem == null) {
            // Empty state
            EmptyMediaPicker(
                isLocked = isLocked,
                onTap = { launcher.launch("*/*") }
            )
        } else {
            PreviewableGrid(
                items = previewableItems,
                canAddMore = canAddMore,
                isLocked = isLocked,
                onAddTap = { launcher.launch("*/*") },
                onRemove = onRemoveMedia
            )
        }

        // Downloadable file section (audio/document/3D)
        if (downloadableItem != null) {
            DownloadableFileCard(
                item = downloadableItem,
                isLocked = isLocked,
                onRemove = { onRemoveMedia(downloadableItem.id) }
            )
        }
    }
}

@Composable
private fun EmptyMediaPicker(
    isLocked: Boolean,
    onTap: () -> Unit
) {
    val shape = RoundedCornerShape(DesperseRadius.md)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (!isLocked) Modifier.clickable(onClick = onTap) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FaIcon(
                FaIcons.Upload,
                size = 32.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(DesperseSpacing.sm))
            Text(
                "Tap to add media",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(DesperseSpacing.xs))
            Text(
                "Images, video, audio, 3D, PDF, ZIP, EPUB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PreviewableGrid(
    items: List<UploadedMediaItem>,
    canAddMore: Boolean,
    isLocked: Boolean,
    onAddTap: () -> Unit,
    onRemove: (String) -> Unit
) {
    val cellShape = RoundedCornerShape(DesperseRadius.sm)
    val columns = 3
    // Calculate row count: items + possible add button
    val totalCells = items.size + if (canAddMore) 1 else 0
    val rows = (totalCells + columns - 1) / columns

    // Use fixed height grid based on row count
    val cellSize = 110.dp // approximate cell size for 3 columns with spacing

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxWidth()
            .height((cellSize + DesperseSpacing.sm) * rows),
        horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm),
        userScrollEnabled = false
    ) {
        items(items, key = { it.id }) { item ->
            PreviewableGridCell(
                item = item,
                isLocked = isLocked,
                cellShape = cellShape,
                onRemove = { onRemove(item.id) }
            )
        }

        if (canAddMore) {
            item(key = "add_more") {
                AddMoreCell(
                    cellShape = cellShape,
                    onTap = onAddTap
                )
            }
        }
    }
}

@Composable
private fun PreviewableGridCell(
    item: UploadedMediaItem,
    isLocked: Boolean,
    cellShape: RoundedCornerShape,
    onRemove: () -> Unit
) {
    val standardColor = toneStandard()
    val destructiveColor = toneDestructive()
    val isVideo = item.mediaType == "video"

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(cellShape)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when (val state = item.uploadState) {
            is UploadState.Idle, is UploadState.Uploading -> {
                // Local preview with upload overlay
                item.localUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Media preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Upload progress overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = standardColor,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            is UploadState.Success -> {
                AsyncImage(
                    model = item.url ?: item.localUri,
                    contentDescription = "Uploaded media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            is UploadState.Failed -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FaIcon(
                            FaIcons.CircleExclamation,
                            size = 20.dp,
                            tint = destructiveColor
                        )
                        Spacer(modifier = Modifier.height(DesperseSpacing.xs))
                        Text(
                            "Failed",
                            style = MaterialTheme.typography.labelSmall,
                            color = destructiveColor
                        )
                    }
                }
            }
        }

        // Video play icon overlay
        if (isVideo && item.uploadState is UploadState.Success) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    FaIcon(
                        FaIcons.CirclePlay,
                        size = 20.dp,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Remove button (top-right)
        if (!isLocked) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(DesperseSpacing.xs)
                    .size(24.dp)
                    .background(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                        CircleShape
                    )
            ) {
                FaIcon(
                    FaIcons.Xmark,
                    size = 10.dp,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun AddMoreCell(
    cellShape: RoundedCornerShape,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(cellShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, cellShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FaIcon(
                FaIcons.Plus,
                size = 20.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(DesperseSpacing.xs))
            Text(
                "Add",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DownloadableFileCard(
    item: UploadedMediaItem,
    isLocked: Boolean,
    onRemove: () -> Unit
) {
    val shape = RoundedCornerShape(DesperseRadius.sm)
    val standardColor = toneStandard()
    val destructiveColor = toneDestructive()

    val fileIcon = remember(item.mimeType) {
        when {
            item.mimeType.contains("pdf") -> FaIcons.FilePdf
            item.mimeType.contains("zip") -> FaIcons.FileZipper
            item.mimeType.contains("epub") -> FaIcons.FileLines
            item.mediaType == "audio" -> FaIcons.Music
            item.mediaType == "3d" -> FaIcons.Cube
            else -> FaIcons.File
        }
    }

    val displayName = item.fileName.ifBlank {
        when (item.mediaType) {
            "audio" -> "Audio file"
            "document" -> "Document"
            "3d" -> "3D model"
            else -> "File"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(DesperseSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(DesperseRadius.xs)
                ),
            contentAlignment = Alignment.Center
        ) {
            FaIcon(
                fileIcon,
                size = 18.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(DesperseSpacing.md))

        // File info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            when (val state = item.uploadState) {
                is UploadState.Idle, is UploadState.Uploading -> {
                    Text(
                        "Uploading...",
                        style = MaterialTheme.typography.bodySmall,
                        color = standardColor
                    )
                }
                is UploadState.Success -> {
                    val sizeText = formatFileSize(item.fileSize)
                    Text(
                        sizeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is UploadState.Failed -> {
                    Text(
                        state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = destructiveColor
                    )
                }
            }
        }

        // Upload progress indicator
        if (item.uploadState is UploadState.Idle || item.uploadState is UploadState.Uploading) {
            CircularProgressIndicator(
                color = standardColor,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(DesperseSpacing.sm))
        }

        // Remove button
        if (!isLocked) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                FaIcon(
                    FaIcons.Xmark,
                    size = 14.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
