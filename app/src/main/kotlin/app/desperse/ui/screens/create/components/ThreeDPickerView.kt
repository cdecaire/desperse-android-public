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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.media.ModelSource
import app.desperse.ui.components.media.ModelViewer
import app.desperse.ui.screens.create.UploadedMediaItem
import app.desperse.ui.theme.DesperseSpacing

/**
 * Full-screen view for 3D file selection.
 * Shows "+ Add 3D file" prompt, or an interactive 3D preview when a file is selected.
 */
@Composable
fun ThreeDPickerView(
    threeDFile: UploadedMediaItem?,
    onAddFile: () -> Unit,
    onRemoveFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center
    ) {
        if (threeDFile == null) {
            // Add 3D file prompt
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable(onClick = onAddFile)
                    .padding(horizontal = DesperseSpacing.xxxl, vertical = DesperseSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
            ) {
                FaIcon(
                    icon = FaIcons.Cube,
                    size = 40.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = FaIconStyle.Regular
                )
                Text(
                    text = "+ Add 3D file",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "GLB or GLTF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val context = LocalContext.current
            val localUri = threeDFile.localUri

            // Interactive 3D preview when we have a local URI
            if (localUri != null) {
                val modelSource = remember(localUri) {
                    ModelSource.Stream {
                        context.contentResolver.openInputStream(localUri)
                            ?: throw IllegalStateException("Cannot open 3D file")
                    }
                }

                ModelViewer(
                    modelSource = modelSource,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback: static icon if no local URI available
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    FaIcon(
                        icon = FaIcons.Cube,
                        size = 32.dp,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = FaIconStyle.Solid
                    )
                }
            }

            // Filename + remove button overlay (bottom)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = DesperseSpacing.xxl)
                    .padding(horizontal = DesperseSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
            ) {
                // Filename pill
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = threeDFile.fileName.ifBlank { "3D file" },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Remove button
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable(onClick = onRemoveFile)
                        .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FaIcon(
                        icon = FaIcons.Xmark,
                        size = 12.dp,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        style = FaIconStyle.Solid
                    )
                    Text(
                        text = "Remove",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
