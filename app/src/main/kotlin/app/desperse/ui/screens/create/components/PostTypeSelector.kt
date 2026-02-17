package app.desperse.ui.screens.create.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val description: String,
    val icon: String,
    val toneColor: Color
)

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
        PostTypeOption("post", "Standard", "Free to view", FaIcons.CirclePlus, standardColor),
        PostTypeOption("collectible", "Collectible", "Free cNFT", FaIcons.Gem, collectibleColor),
        PostTypeOption("edition", "Edition", "Paid NFT", FaIcons.LayerGroup, editionColor)
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

        postTypeOptions.forEach { option ->
            val isSelected = option.type == selectedType
            val shape = RoundedCornerShape(DesperseRadius.md)
            val borderColor = if (isSelected) option.toneColor else MaterialTheme.colorScheme.outline

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = borderColor,
                        shape = shape
                    )
                    .clickable { onTypeSelected(option.type) },
                color = if (isSelected) option.toneColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(DesperseSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FaIcon(
                        icon = option.icon,
                        size = 20.dp,
                        tint = if (isSelected) option.toneColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(DesperseSpacing.md))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            option.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) option.toneColor else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            option.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = { onTypeSelected(option.type) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = option.toneColor,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
