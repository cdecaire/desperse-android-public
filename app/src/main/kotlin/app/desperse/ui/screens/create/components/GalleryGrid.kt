package app.desperse.ui.screens.create.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.desperse.data.repository.DeviceMediaItem
import app.desperse.ui.screens.create.MediaTab
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * 4-column grid of device media items.
 * Image tab: always multi-select with numbered badges.
 * Video/Audio tab: single-select with highlight border.
 */
@Composable
fun GalleryGrid(
    items: List<DeviceMediaItem>,
    selectedItems: List<DeviceMediaItem>,
    mediaTab: MediaTab = MediaTab.Image,
    onItemClick: (DeviceMediaItem) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMultiSelect = mediaTab == MediaTab.Image
    val gridState = rememberLazyGridState()

    // Infinite scroll
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (lastVisible >= totalItems - 12 && totalItems > 0) {
                    onLoadMore()
                }
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(
            items = items,
            key = { "${it.id}_${if (it.isVideo) "v" else "i"}" },
            contentType = { "gallery_item" }
        ) { item ->
            val selectionIndex = selectedItems.indexOfFirst { it.id == item.id && it.isVideo == item.isVideo }
            val isSelected = selectionIndex >= 0

            GalleryGridCell(
                item = item,
                isSelected = isSelected,
                isMultiSelect = isMultiSelect,
                selectionNumber = if (isMultiSelect && isSelected) selectionIndex + 1 else null,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun GalleryGridCell(
    item: DeviceMediaItem,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    selectionNumber: Int?,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(1.dp))
            .clickable { onClick() }
            .then(
                if (isSelected && !isMultiSelect) {
                    // Single-select highlight border
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
                } else Modifier
            )
    ) {
        // Thumbnail
        AsyncImage(
            model = remember(item.uri) {
                ImageRequest.Builder(context)
                    .data(item.uri)
                    .size(256)
                    .crossfade(true)
                    .build()
            },
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Low
        )

        // Semi-transparent overlay when selected in multi-select
        if (isSelected && isMultiSelect) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }

        // Video duration overlay (bottom-right)
        if (item.isVideo && item.duration > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = formatDuration(item.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }

        // Multi-select badge (top-right)
        if (isMultiSelect) {
            if (selectionNumber != null) {
                // Selected: filled circle with number
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectionNumber.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            } else {
                // Unselected: empty ring
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
