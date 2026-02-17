package app.desperse.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.desperse.BuildConfig
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.DesperseTones
import kotlinx.coroutines.launch

private const val MAX_MESSAGE_LENGTH = 1000

/**
 * Beta Feedback Modal Sheet
 *
 * Lightweight feedback form with:
 * - Star rating (1-5, optional, tap to toggle)
 * - Message textarea (1000 char max, optional)
 * - Requires at least ONE field (rating or message)
 *
 * Note: Screenshot upload is deferred for v2 (requires image picker + upload flow)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSubmit: suspend (rating: Int?, message: String?, appVersion: String?, userAgent: String?) -> Result<Unit>
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var rating by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val trimmedMessage = message.trim()
    val hasContent = rating != null || trimmedMessage.isNotEmpty()
    val canSubmit = hasContent && !isSubmitting

    fun resetForm() {
        rating = null
        message = ""
    }

    fun submit() {
        if (!canSubmit) return
        isSubmitting = true

        scope.launch {
            val appVersion = BuildConfig.VERSION_NAME
            val userAgent = "Desperse Android/${BuildConfig.VERSION_NAME} (${Build.MODEL}; Android ${Build.VERSION.SDK_INT})"

            val result = onSubmit(
                rating,
                trimmedMessage.ifEmpty { null },
                appVersion,
                userAgent
            )

            isSubmitting = false

            result.onSuccess {
                resetForm()
                onDismiss()
            }
            // Error handling is done via toast in the ViewModel
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isSubmitting) {
                resetForm()
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = DesperseSpacing.md),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesperseSpacing.lg)
                .padding(bottom = DesperseSpacing.xxl)
        ) {
            // Header
            Text(
                text = "Beta feedback",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(DesperseSpacing.xs))
            Text(
                text = "Anything helps. Bugs, ideas, suggestions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.lg))

            // Star Rating
            Text(
                text = "How's it going?",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(DesperseSpacing.sm))
            StarRating(
                value = rating,
                onChange = { newRating ->
                    // Toggle: if clicking same star, clear it
                    rating = if (rating == newRating) null else newRating
                },
                enabled = !isSubmitting
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.lg))

            // Message Field
            Text(
                text = "Message",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(DesperseSpacing.sm))
            OutlinedTextField(
                value = message,
                onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) message = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
                placeholder = {
                    Text(
                        text = "What happened? What were you trying to do?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                minLines = 4,
                maxLines = 6,
                supportingText = {
                    Text(
                        text = "${message.length} / $MAX_MESSAGE_LENGTH",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.xl))

            // Submit Button
            Button(
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSubmit,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(DesperseSpacing.sm))
                    Text("Sending...")
                } else {
                    Text("Send feedback")
                }
            }
        }
    }
}

/**
 * Star Rating Component
 * Tap to select, tap same star to clear
 */
@Composable
private fun StarRating(
    value: Int?,
    onChange: (Int) -> Unit,
    enabled: Boolean = true
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { star ->
            val isSelected = (value ?: 0) >= star

            Box(
                modifier = Modifier
                    .size(DesperseSizes.minTouchTarget)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled) { onChange(star) },
                contentAlignment = Alignment.Center
            ) {
                FaIcon(
                    icon = if (isSelected) FaIcons.StarSolid else FaIcons.Star,
                    size = 28.dp,
                    style = if (isSelected) FaIconStyle.Solid else FaIconStyle.Regular,
                    tint = if (isSelected) {
                        Color(0xFFFFC107) // Yellow for selected stars
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // Clear button when rating is set
        if (value != null && enabled) {
            Spacer(modifier = Modifier.width(DesperseSpacing.sm))
            Text(
                text = "Clear",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onChange(value) } // Clicking current value clears
                    .padding(horizontal = DesperseSpacing.sm, vertical = DesperseSpacing.xs)
            )
        }
    }
}
