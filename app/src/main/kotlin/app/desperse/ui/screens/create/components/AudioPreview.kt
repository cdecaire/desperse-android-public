package app.desperse.ui.screens.create.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.desperse.data.repository.DeviceMediaItem
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.screens.create.UploadedMediaItem
import app.desperse.ui.theme.DesperseSpacing
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Preview area content for the Audio tab.
 * Shows cover image background (if selected) with audio file overlay.
 * When no audio file is selected, shows "+ Add audio" prompt.
 * When audio file is loaded, shows filename with remove button.
 */
@Composable
fun AudioPreview(
    coverItem: DeviceMediaItem?,
    audioFile: UploadedMediaItem?,
    onAddAudio: () -> Unit,
    onRemoveAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Cover image background
        if (coverItem != null) {
            AsyncImage(
                model = remember(coverItem.uri) {
                    ImageRequest.Builder(context)
                        .data(coverItem.uri)
                        .crossfade(true)
                        .build()
                },
                contentDescription = "Cover image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Audio overlay
        if (audioFile == null) {
            // "+ Add audio" prompt
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onAddAudio)
                    .padding(horizontal = DesperseSpacing.xxl, vertical = DesperseSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
            ) {
                FaIcon(
                    icon = FaIcons.Music,
                    size = 28.dp,
                    tint = Color.White,
                    style = FaIconStyle.Solid
                )
                Text(
                    text = "+ Add audio",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        } else {
            // Audio file control bar at bottom
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
            ) {
                // Music icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    FaIcon(
                        icon = FaIcons.Music,
                        size = 16.dp,
                        tint = MaterialTheme.colorScheme.primary,
                        style = FaIconStyle.Solid
                    )
                }

                // Filename
                Text(
                    text = audioFile.fileName.ifBlank { "Audio file" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Remove button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable(onClick = onRemoveAudio),
                    contentAlignment = Alignment.Center
                ) {
                    FaIcon(
                        icon = FaIcons.Xmark,
                        size = 12.dp,
                        tint = Color.White,
                        style = FaIconStyle.Solid
                    )
                }
            }
        }

        // Hint when no cover and no audio
        if (coverItem == null && audioFile == null) {
            Text(
                text = "Select a cover image below",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = DesperseSpacing.lg)
            )
        }
    }
}
