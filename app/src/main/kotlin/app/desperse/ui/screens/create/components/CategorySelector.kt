package app.desperse.ui.screens.create.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.desperse.data.model.Categories
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.toneInfo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelector(
    selectedCategories: List<String>,
    onToggle: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val checkColor = toneInfo()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
    ) {
        // Header row: label + counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Categories (optional)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${selectedCategories.size}/${Categories.MAX_CATEGORIES} selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Selector box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(DesperseRadius.sm)
                )
                .clickable(enabled = enabled) { isExpanded = !isExpanded },
            shape = RoundedCornerShape(DesperseRadius.sm),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = DesperseSpacing.md, vertical = DesperseSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selected chips or placeholder
                if (selectedCategories.isEmpty()) {
                    Text(
                        "Select categories...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
                    ) {
                        selectedCategories.forEach { category ->
                            InputChip(
                                selected = true,
                                onClick = { onToggle(category) },
                                label = {
                                    Text(
                                        category,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                trailingIcon = {
                                    FaIcon(
                                        FaIcons.Xmark,
                                        size = 10.dp,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = InputChipDefaults.inputChipBorder(
                                    selectedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    enabled = true,
                                    selected = true
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.width(DesperseSpacing.sm))

                // Chevron
                FaIcon(
                    icon = if (isExpanded) FaIcons.ChevronUp else FaIcons.ChevronDown,
                    size = 14.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Dropdown list
        if (isExpanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(DesperseRadius.sm)
                    ),
                shape = RoundedCornerShape(DesperseRadius.sm),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp)
                ) {
                    items(Categories.PRESETS) { category ->
                        val isSelected = category in selectedCategories
                        val canSelect = isSelected || selectedCategories.size < Categories.MAX_CATEGORIES

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = enabled && canSelect) {
                                    onToggle(category)
                                }
                                .padding(
                                    horizontal = DesperseSpacing.lg,
                                    vertical = DesperseSpacing.md
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!canSelect && !isSelected)
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                FaIcon(
                                    FaIcons.Check,
                                    size = 16.dp,
                                    tint = checkColor
                                )
                            }
                        }

                        if (category != Categories.PRESETS.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}
