package app.desperse.ui.screens.create.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.desperse.ui.components.DesperseBottomSheet
import app.desperse.ui.theme.DesperseSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimedEditionSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    enabled: Boolean,
    startMode: String,
    startTime: Long?,
    durationHours: Double?,
    isLocked: Boolean,
    onToggle: (Boolean) -> Unit,
    onStartModeChange: (String) -> Unit,
    onStartTimeChange: (Long) -> Unit,
    onDurationChange: (Double?) -> Unit,
    isDoneEnabled: Boolean = true
) {
    DesperseBottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesperseSpacing.lg)
                .padding(bottom = DesperseSpacing.xxl)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DesperseSpacing.lg)
        ) {
            Text(
                text = "Timed Edition",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            TimedEditionSection(
                enabled = enabled,
                startMode = startMode,
                startTime = startTime,
                durationHours = durationHours,
                isLocked = isLocked,
                onToggle = onToggle,
                onStartModeChange = onStartModeChange,
                onStartTimeChange = onStartTimeChange,
                onDurationChange = onDurationChange
            )

            Spacer(modifier = Modifier.height(DesperseSpacing.sm))
            Button(
                onClick = onDismiss,
                enabled = isDoneEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
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
