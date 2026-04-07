package app.desperse.ui.screens.create.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.desperse.data.repository.DeviceAudioItem
import app.desperse.ui.components.DesperseBottomSheet
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseSpacing

/**
 * Bottom sheet listing audio files from the device with search.
 * Replaces the system file picker for a consistent, dismissable UX.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPickerSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    audioFiles: List<DeviceAudioItem>,
    isLoading: Boolean,
    onSelect: (DeviceAudioItem) -> Unit
) {
    DesperseBottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = DesperseSpacing.xxl)
        ) {
            Text(
                text = "Select audio file",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    horizontal = DesperseSpacing.lg,
                    vertical = DesperseSpacing.sm
                )
            )

            // Search bar
            var searchQuery by remember { mutableStateOf("") }
            val filteredFiles by remember(audioFiles, searchQuery) {
                derivedStateOf {
                    if (searchQuery.isBlank()) audioFiles
                    else {
                        val q = searchQuery.lowercase()
                        audioFiles.filter {
                            it.displayName.lowercase().contains(q) ||
                                it.relativePath.lowercase().contains(q)
                        }
                    }
                }
            }

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search audio files...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    FaIcon(
                        icon = FaIcons.MagnifyingGlass,
                        size = 14.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = FaIconStyle.Solid
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm)
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
                filteredFiles.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
                        ) {
                            FaIcon(
                                icon = FaIcons.Music,
                                size = 32.dp,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = FaIconStyle.Regular
                            )
                            Text(
                                text = if (searchQuery.isBlank()) "No audio files found"
                                       else "No results for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(
                            items = filteredFiles,
                            key = { it.id }
                        ) { audioItem ->
                            AudioFileRow(
                                item = audioItem,
                                onClick = {
                                    onSelect(audioItem)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioFileRow(
    item: DeviceAudioItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
    ) {
        // Music icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            FaIcon(
                icon = FaIcons.Music,
                size = 16.dp,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                style = FaIconStyle.Solid
            )
        }

        // File info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
            ) {
                // Folder path
                if (item.relativePath.isNotBlank()) {
                    val folder = item.relativePath.trimEnd('/')
                    Text(
                        text = folder,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (item.duration > 0) {
                    Text(
                        text = formatDuration(item.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.fileSize > 0) {
                    Text(
                        text = formatFileSize(item.fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}
