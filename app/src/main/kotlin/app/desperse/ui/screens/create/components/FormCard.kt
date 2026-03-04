package app.desperse.ui.screens.create.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing

/**
 * Reusable card wrapper matching web create page styling:
 * rounded-xl (20dp), 1dp border, 16dp padding, surface color, subtle shadow.
 */
@Composable
fun FormCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(DesperseRadius.xl),
                clip = false
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(DesperseRadius.xl)
            ),
        shape = RoundedCornerShape(DesperseRadius.xl),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(DesperseSpacing.lg),
            content = content
        )
    }
}
