package app.desperse.ui.screens.create.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.desperse.ui.components.DesperseBottomSheet
import app.desperse.ui.screens.create.UploadedMediaItem
import app.desperse.ui.theme.DesperseSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    attachments: List<UploadedMediaItem>,
    onAdd: (Uri) -> Unit,
    onRemove: (String) -> Unit,
    protectDownload: Boolean = false,
    onProtectDownloadChange: ((Boolean) -> Unit)? = null
) {
    DesperseBottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesperseSpacing.lg)
                .padding(bottom = DesperseSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(DesperseSpacing.lg)
        ) {
            Text(
                text = "Attachments",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            AttachmentSection(
                attachments = attachments,
                onAdd = onAdd,
                onRemove = onRemove,
                protectDownload = protectDownload,
                onProtectDownloadChange = onProtectDownloadChange
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.sm))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    "Done",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
