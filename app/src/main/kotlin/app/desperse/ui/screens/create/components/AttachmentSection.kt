package app.desperse.ui.screens.create.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.desperse.data.upload.UploadState
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.screens.create.UploadedMediaItem
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.toneDestructive

private val ATTACHMENT_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/zip",
    "application/x-zip-compressed",
    "application/epub+zip"
)

@Composable
fun AttachmentSection(
    attachments: List<UploadedMediaItem>,
    onAdd: (Uri) -> Unit,
    onRemove: (String) -> Unit,
    protectDownload: Boolean = false,
    onProtectDownloadChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onAdd(it) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
    ) {
        Text(
            "Add a downloadable file for collectors (PDF, ZIP, EPUB)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Show existing attachment or add button
        val attachment = attachments.firstOrNull()
        if (attachment != null) {
            AttachmentRow(item = attachment, onRemove = { onRemove(attachment.id) })

            // Protect download toggle (shown when attachment uploaded and callback provided)
            if (onProtectDownloadChange != null && attachment.url != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Protect download",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Require purchase before downloading",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = protectDownload,
                        onCheckedChange = onProtectDownloadChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.surface,
                            checkedTrackColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        } else {
            // Add button
            val shape = RoundedCornerShape(DesperseRadius.md)
            Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = shape
                )
                .clickable { filePicker.launch(ATTACHMENT_MIME_TYPES) },
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(DesperseSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                FaIcon(
                    icon = FaIcons.Plus,
                    size = 14.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(DesperseSpacing.sm))
                Text(
                    "Add file",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }
    }
}

@Composable
private fun AttachmentRow(
    item: UploadedMediaItem,
    onRemove: () -> Unit
) {
    val destructiveColor = toneDestructive()
    val shape = RoundedCornerShape(DesperseRadius.md)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(DesperseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File type icon
            val icon = when {
                item.mimeType.contains("pdf") -> FaIcons.FilePdf
                item.mimeType.contains("zip") -> FaIcons.FileZipper
                item.mimeType.contains("epub") -> FaIcons.File
                else -> FaIcons.File
            }
            FaIcon(
                icon = icon,
                size = 18.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(DesperseSpacing.md))

            // File name and size
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.fileName.ifBlank { "Uploading..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                when (item.uploadState) {
                    is UploadState.Uploading -> {
                        Text(
                            "Uploading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is UploadState.Failed -> {
                        Text(
                            item.uploadState.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = destructiveColor
                        )
                    }
                    else -> {
                        if (item.fileSize > 0) {
                            Text(
                                formatFileSize(item.fileSize),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Upload progress or remove button
            when (item.uploadState) {
                is UploadState.Uploading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    FaIcon(
                        icon = FaIcons.Xmark,
                        size = 14.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onRemove() }
                            .padding(DesperseSpacing.xs)
                    )
                }
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
