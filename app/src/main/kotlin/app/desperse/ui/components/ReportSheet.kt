package app.desperse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.desperse.ui.components.media.ImageContext
import app.desperse.ui.components.media.ImageOptimization
import app.desperse.ui.theme.DesperseSpacing
import coil.compose.AsyncImage

/**
 * Report reasons matching web app
 */
val REPORT_REASONS = listOf(
    "Copyright infringement",
    "Fraud / scam",
    "Hate speech",
    "Abuse & harassment",
    "Privacy concern",
    "Other"
)

/**
 * Content preview for the report modal
 */
data class ReportContentPreview(
    val userName: String,
    val userAvatarUrl: String? = null,
    val contentText: String? = null,
    val mediaUrl: String? = null
)

/**
 * ReportSheet - Modal bottom sheet for reporting content
 *
 * Features:
 * - Content preview (avatar, name, snippet)
 * - Multi-select reason checkboxes
 * - Optional details textarea (required for "Other")
 * - Loading state during submission
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSheet(
    open: Boolean,
    onDismiss: () -> Unit,
    contentType: String, // "post" or "comment"
    contentPreview: ReportContentPreview,
    onSubmit: suspend (reasons: List<String>, details: String?) -> Result<Unit>,
    modifier: Modifier = Modifier
) {
    if (!open) return

    var selectedReasons by remember { mutableStateOf(setOf<String>()) }
    var details by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val requiresDetails = "Other" in selectedReasons
    val canSubmit = selectedReasons.isNotEmpty() &&
                    (!requiresDetails || details.isNotBlank()) &&
                    !isSubmitting

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesperseSpacing.lg)
                .padding(bottom = DesperseSpacing.xl)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = "Report ${if (contentType == "post") "Post" else "Comment"}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.md))

            // Content preview
            ContentPreviewCard(preview = contentPreview)

            Spacer(modifier = Modifier.height(DesperseSpacing.lg))

            // Reason selection
            Text(
                text = "Why are you reporting this?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Select all that apply",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.sm))

            // Reason checkboxes
            REPORT_REASONS.forEach { reason ->
                val isSelected = reason in selectedReasons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selectedReasons = if (isSelected) {
                                selectedReasons - reason
                            } else {
                                selectedReasons + reason
                            }
                        }
                        .padding(vertical = DesperseSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            selectedReasons = if (checked) {
                                selectedReasons + reason
                            } else {
                                selectedReasons - reason
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(DesperseSpacing.xs))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Details field (required when "Other" is selected)
            if (requiresDetails || details.isNotBlank()) {
                Spacer(modifier = Modifier.height(DesperseSpacing.md))
                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 500) details = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(if (requiresDetails) "Please provide details *" else "Additional details (optional)")
                    },
                    placeholder = { Text("Describe the issue...") },
                    minLines = 3,
                    maxLines = 5,
                    supportingText = {
                        Text("${details.length}/500")
                    },
                    isError = requiresDetails && details.isBlank()
                )
            }

            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(DesperseSpacing.sm))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(DesperseSpacing.lg))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        isSubmitting = true
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canSubmit
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Report")
                    }
                }
            }
        }
    }

    // Handle submission
    LaunchedEffect(isSubmitting) {
        if (isSubmitting) {
            val result = onSubmit(
                selectedReasons.toList(),
                details.ifBlank { null }
            )
            result.onSuccess {
                onDismiss()
            }.onFailure { error ->
                errorMessage = error.message ?: "Failed to submit report"
                isSubmitting = false
            }
        }
    }
}

@Composable
private fun ContentPreviewCard(
    preview: ReportContentPreview
) {
    val optimizedAvatarUrl = remember(preview.userAvatarUrl) {
        preview.userAvatarUrl?.let {
            ImageOptimization.getOptimizedUrlForContext(it, ImageContext.AVATAR)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(DesperseSpacing.md),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (optimizedAvatarUrl != null) {
                    AsyncImage(
                        model = optimizedAvatarUrl,
                        contentDescription = preview.userName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    FaIcon(
                        icon = FaIcons.User,
                        size = 18.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preview.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!preview.contentText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = preview.contentText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Media thumbnail (if post has media)
            if (preview.mediaUrl != null) {
                val optimizedMediaUrl = remember(preview.mediaUrl) {
                    ImageOptimization.getOptimizedUrlForContext(preview.mediaUrl, ImageContext.AVATAR)
                }
                AsyncImage(
                    model = optimizedMediaUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
