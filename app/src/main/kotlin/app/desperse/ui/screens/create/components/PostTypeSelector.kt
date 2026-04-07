package app.desperse.ui.screens.create.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.toneCollectible
import app.desperse.ui.theme.toneEdition
import app.desperse.ui.theme.toneStandard

data class PostTypeOption(
    val type: String,
    val title: String,
    val icon: String,
    val toneColor: Color
)

/**
 * Post type selector displayed as 3 equal columns.
 * Standard is pre-selected. Tapping switches selection.
 */
@Composable
fun PostTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val standardColor = toneStandard()
    val collectibleColor = toneCollectible()
    val editionColor = toneEdition()

    val postTypeOptions = listOf(
        PostTypeOption("post", "Standard", FaIcons.CirclePlus, standardColor),
        PostTypeOption("collectible", "Collectible", FaIcons.Gem, collectibleColor),
        PostTypeOption("edition", "Edition", FaIcons.LayerGroup, editionColor)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
    ) {
        Text(
            "Post Type",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
        ) {
            postTypeOptions.forEach { option ->
                val isSelected = option.type == selectedType
                val shape = RoundedCornerShape(DesperseRadius.md)

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) option.toneColor else MaterialTheme.colorScheme.outlineVariant,
                            shape = shape
                        )
                        .clickable { onTypeSelected(option.type) },
                    color = if (isSelected) option.toneColor.copy(alpha = 0.08f)
                           else MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = DesperseSpacing.md, horizontal = DesperseSpacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
                    ) {
                        FaIcon(
                            icon = option.icon,
                            size = 18.dp,
                            tint = if (isSelected) option.toneColor
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            option.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) option.toneColor
                                   else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
