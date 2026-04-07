package app.desperse.ui.screens.create.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.desperse.data.model.Categories
import app.desperse.ui.components.DesperseBottomSheet
import app.desperse.ui.theme.DesperseSpacing

/**
 * Bottom sheet for selecting post categories with checkboxes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    selectedCategories: List<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit
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
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${selectedCategories.size}/${Categories.MAX_CATEGORIES}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Select up to ${Categories.MAX_CATEGORIES} categories",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DesperseSpacing.xs, bottom = DesperseSpacing.md)
            )

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(Categories.PRESETS) { category ->
                    val isSelected = category in selectedCategories
                    val canSelect = isSelected || selectedCategories.size < Categories.MAX_CATEGORIES

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled && canSelect) { onToggle(category) }
                            .padding(vertical = DesperseSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggle(category) },
                            enabled = enabled && canSelect
                        )
                        Text(
                            category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (!canSelect && !isSelected)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (category != Categories.PRESETS.last()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // Done button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DesperseSpacing.lg),
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
