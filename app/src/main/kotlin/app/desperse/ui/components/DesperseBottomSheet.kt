package app.desperse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing

/**
 * Standard drag handle used across all bottom sheets.
 */
@Composable
fun SheetDragHandle() {
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

/**
 * Desperse-styled ModalBottomSheet with standard drag handle, colors, and setup.
 *
 * @param isOpen Whether the sheet is visible
 * @param onDismiss Called when the sheet is dismissed
 * @param sheetState Optional custom sheet state
 * @param onDismissRequest Custom dismiss handler (defaults to onDismiss). Use to prevent dismissal during submission.
 * @param contentWindowInsets Custom window insets for the sheet content
 * @param content Sheet content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesperseBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: (() -> Unit)? = null,
    contentWindowInsets: (@Composable () -> WindowInsets)? = null,
    content: @Composable () -> Unit
) {
    if (isOpen) {
        if (contentWindowInsets != null) {
            ModalBottomSheet(
                onDismissRequest = onDismissRequest ?: onDismiss,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                contentWindowInsets = contentWindowInsets,
                dragHandle = { SheetDragHandle() }
            ) {
                content()
            }
        } else {
            ModalBottomSheet(
                onDismissRequest = onDismissRequest ?: onDismiss,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                dragHandle = { SheetDragHandle() }
            ) {
                content()
            }
        }
    }
}

/**
 * Reusable menu item row for bottom sheets.
 * Supports FontAwesome icon + label + optional subtitle + optional trailing content.
 *
 * @param icon FontAwesome icon unicode (from FaIcons)
 * @param label Primary text
 * @param onClick Click handler
 * @param subtitle Optional secondary text below label
 * @param iconStyle FA icon style (default: Regular)
 * @param tint Icon and text color override
 * @param enabled Whether the item is clickable
 * @param trailing Optional trailing composable (e.g., chevron, badge)
 */
@Composable
fun SheetMenuItem(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconStyle: FaIconStyle = FaIconStyle.Regular,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(
                horizontal = DesperseSpacing.lg,
                vertical = DesperseSpacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FaIcon(
            icon = icon,
            size = DesperseSizes.iconMd,
            style = iconStyle,
            tint = if (enabled) tint else tint.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.width(DesperseSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) tint else tint.copy(alpha = 0.5f)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailing != null) {
            trailing()
        }
    }
}
