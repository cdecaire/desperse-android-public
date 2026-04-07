package app.desperse.ui.screens.create.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.desperse.data.repository.DeviceMediaRepository
import app.desperse.data.repository.MediaAlbum
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseSpacing

/**
 * Album filter dropdown and multi-select toggle for the gallery grid.
 */
@Composable
fun AlbumFilterBar(
    albums: List<MediaAlbum>,
    selectedAlbumId: String?,
    isMultiSelect: Boolean,
    onAlbumSelected: (String?) -> Unit,
    onMultiSelectToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }

    val selectedAlbumName = remember(selectedAlbumId, albums) {
        when (selectedAlbumId) {
            null, DeviceMediaRepository.ALBUM_RECENTS -> "Recents"
            else -> albums.find { it.id == selectedAlbumId }?.displayName ?: "Recents"
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Album dropdown
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDropdown = true }
                    .padding(horizontal = DesperseSpacing.sm, vertical = DesperseSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
            ) {
                Text(
                    text = selectedAlbumName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                FaIcon(
                    icon = FaIcons.ChevronDown,
                    size = 12.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                    style = FaIconStyle.Solid
                )
            }

            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                albums.forEach { album ->
                    val isSelected = album.id == (selectedAlbumId ?: DeviceMediaRepository.ALBUM_RECENTS)
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = album.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "${album.count}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            val id = if (album.id == DeviceMediaRepository.ALBUM_RECENTS) null else album.id
                            onAlbumSelected(id)
                            showDropdown = false
                        }
                    )
                }
            }
        }

        // Multi-select toggle
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (isMultiSelect) {
                        Modifier.background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                    } else Modifier
                )
                .clickable { onMultiSelectToggle() }
                .padding(horizontal = DesperseSpacing.sm, vertical = DesperseSpacing.xs),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
            ) {
                FaIcon(
                    icon = FaIcons.Images,
                    size = 16.dp,
                    tint = if (isMultiSelect) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface,
                    style = FaIconStyle.Regular
                )
                Text(
                    text = if (isMultiSelect) "Cancel" else "Select",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isMultiSelect) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
