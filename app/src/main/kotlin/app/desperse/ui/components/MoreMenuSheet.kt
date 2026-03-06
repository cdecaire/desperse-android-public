package app.desperse.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    onWhatsNewClick: () -> Unit,
    onLogoutClick: () -> Unit,
    isAuthenticated: Boolean = true
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
            if (isAuthenticated) {
                SheetMenuItem(
                    icon = FaIcons.Gear,
                    label = "Settings",
                    subtitle = "Account, preferences, help",
                    onClick = {
                        onDismiss()
                        onSettingsClick()
                    }
                )

                SheetMenuItem(
                    icon = FaIcons.CircleInfo,
                    label = "Help",
                    onClick = {
                        onDismiss()
                        onHelpClick()
                    }
                )

                SheetMenuItem(
                    icon = FaIcons.MessageLines,
                    label = "Send beta feedback",
                    onClick = {
                        onDismiss()
                        onFeedbackClick()
                    }
                )

                SheetMenuItem(
                    icon = FaIcons.FileLines,
                    label = "What's new",
                    subtitle = "v${app.desperse.BuildConfig.VERSION_NAME}",
                    onClick = {
                        onDismiss()
                        onWhatsNewClick()
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(
                        horizontal = DesperseSpacing.lg,
                        vertical = DesperseSpacing.xs
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                SheetMenuItem(
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
