package app.desperse.ui.screens.create.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.desperse.ui.screens.create.MediaTab
import app.desperse.ui.theme.DesperseSpacing

private data class MediaTabOption(
    val tab: MediaTab,
    val label: String
)

private val mediaTabOptions = listOf(
    MediaTabOption(MediaTab.Image, "Image"),
    MediaTabOption(MediaTab.Video, "Video"),
    MediaTabOption(MediaTab.Audio, "Audio"),
    MediaTabOption(MediaTab.ThreeD, "3D")
)

/**
 * Floating pill-shaped segment control for selecting media type.
 * Positioned at the bottom of the gallery grid area.
 */
@Composable
fun MediaTypeSelector(
    selectedTab: MediaTab,
    onTabSelected: (MediaTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(8.dp, CircleShape)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(horizontal = DesperseSpacing.xs, vertical = DesperseSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.micro),
        verticalAlignment = Alignment.CenterVertically
    ) {
        mediaTabOptions.forEach { option ->
            val isSelected = option.tab == selectedTab
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                label = "tabBg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tabContent"
            )
            val onClick = remember(option.tab) { { onTabSelected(option.tab) } }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable(onClick = onClick)
                    .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = contentColor
                )
            }
        }
    }
}
