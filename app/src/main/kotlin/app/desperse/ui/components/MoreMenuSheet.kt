package app.desperse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing

/**
 * More Menu Bottom Sheet
 *
 * Native-feeling bottom sheet with menu options matching web design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    isAuthenticated: Boolean = true
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = {
                // Custom drag handle
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
                    .padding(bottom = DesperseSpacing.xxl)
            ) {
                if (isAuthenticated) {
                    // Settings
                    MoreMenuItem(
                        icon = FaIcons.Gear,
                        label = "Settings",
                        subtitle = "Account, preferences, help",
                        onClick = {
                            onDismiss()
                            onSettingsClick()
                        }
                    )

                    // Help
                    MoreMenuItem(
                        icon = FaIcons.CircleInfo,
                        label = "Help",
                        onClick = {
                            onDismiss()
                            onHelpClick()
                        }
                    )

                    // Send beta feedback
                    MoreMenuItem(
                        icon = FaIcons.MessageLines,
                        label = "Send beta feedback",
                        onClick = {
                            onDismiss()
                            onFeedbackClick()
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(
                            horizontal = DesperseSpacing.lg,
                            vertical = DesperseSpacing.xs
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Sign out
                    MoreMenuItem(
                        icon = FaIcons.ArrowRightFromBracket,
                        label = "Sign out",
                        onClick = {
                            onDismiss()
                            onLogoutClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreMenuItem(
    icon: String,
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(
                horizontal = DesperseSpacing.lg,
                vertical = DesperseSpacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FaIcon(
            icon = icon,
            size = DesperseSizes.iconMd,
            style = FaIconStyle.Regular,
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.width(DesperseSpacing.md))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
