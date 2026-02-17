package app.desperse.ui.screens.create.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.desperse.data.upload.UploadState
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.screens.create.UploadedMediaItem
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.toneDestructive
import app.desperse.ui.theme.toneStandard
import coil.compose.AsyncImage

@Composable
fun MediaPickerCard(
    primaryMedia: UploadedMediaItem?,
    isLocked: Boolean,
    onMediaSelected: (Uri) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onMediaSelected(it) }
    }

    val shape = RoundedCornerShape(DesperseRadius.md)
    val standardColor = toneStandard()
    val destructiveColor = toneDestructive()

    if (primaryMedia == null) {
        // Empty state - tap to select
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(shape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = shape
                )
                .background(MaterialTheme.colorScheme.surface)
                .then(if (!isLocked) Modifier.clickable { launcher.launch("*/*") } else Modifier),
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
                    "Images, video, audio, 3D, PDF, ZIP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        // Has media - show preview
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(shape)
        ) {
            when (val state = primaryMedia.uploadState) {
                is UploadState.Idle, is UploadState.Uploading -> {
                    // Show local preview with upload progress
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        primaryMedia.localUri?.let { uri ->
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = standardColor,
                                    modifier = Modifier.size(32.dp)
                                )
                                if (state is UploadState.Uploading) {
                                    Spacer(modifier = Modifier.height(DesperseSpacing.sm))
                                    LinearProgressIndicator(
                                        progress = { state.progress },
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = standardColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                is UploadState.Success -> {
                    // Show uploaded preview
                    AsyncImage(
                        model = primaryMedia.url ?: primaryMedia.localUri,
                        contentDescription = "Uploaded media",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                is UploadState.Failed -> {
                    // Error state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { primaryMedia.localUri?.let { onMediaSelected(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FaIcon(
                                FaIcons.CircleExclamation,
                                size = 32.dp,
                                tint = destructiveColor
                            )
                            Spacer(modifier = Modifier.height(DesperseSpacing.sm))
                            Text(
                                state.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = destructiveColor
                            )
                            Spacer(modifier = Modifier.height(DesperseSpacing.xs))
                            Text(
                                "Tap to retry",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Remove button (top-right)
            if (!isLocked && primaryMedia.url != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(DesperseSpacing.sm)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f), CircleShape)
                ) {
                    FaIcon(
                        FaIcons.Xmark,
                        size = 14.dp,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CoverPickerCard(
    coverMedia: UploadedMediaItem?,
    onCoverSelected: (Uri) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onCoverSelected(it) }
    }

    val shape = RoundedCornerShape(DesperseRadius.sm)
    val standardColor = toneStandard()

    if (coverMedia == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outline, shape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FaIcon(FaIcons.Image, size = 24.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(DesperseSpacing.xs))
                Text("Add cover image", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(shape)
        ) {
            AsyncImage(
                model = coverMedia.url ?: coverMedia.localUri,
                contentDescription = "Cover image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (coverMedia.uploadState is UploadState.Uploading) {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = standardColor, modifier = Modifier.size(24.dp))
                }
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(DesperseSpacing.xs)
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f), CircleShape)
            ) {
                FaIcon(FaIcons.Xmark, size = 12.dp, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
