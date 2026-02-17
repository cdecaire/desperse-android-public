package app.desperse.ui.screens.create.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.desperse.data.model.Categories
import app.desperse.ui.theme.DesperseSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelector(
    selectedCategories: List<String>,
    onToggle: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
    ) {
        Text(
            "Categories (${selectedCategories.size}/${Categories.MAX_CATEGORIES})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
        ) {
            Categories.PRESETS.forEach { category ->
                val isSelected = category in selectedCategories
                val canSelect = isSelected || selectedCategories.size < Categories.MAX_CATEGORIES

                FilterChip(
                    selected = isSelected,
                    onClick = { if (enabled && canSelect) onToggle(category) },
                    label = {
                        Text(
                            category,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    enabled = enabled && canSelect,
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        enabled = enabled && canSelect,
                        selected = isSelected
                    )
                )
            }
        }
    }
}
