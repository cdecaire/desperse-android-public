package app.desperse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.desperse.ui.theme.DesperseColors
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseTones

/**
 * Badge variants matching style guide
 */
enum class BadgeVariant {
    Default,      // Primary color with 10% opacity bg
    Secondary,    // Surface variant bg
    Destructive,  // Error/red
    Success,      // Green
    Warning,      // Orange
    Collectible,  // Blue-gem (free NFTs)
    Edition,      // Purple-heart (paid NFTs)
    Outline       // Transparent with border
}

/**
 * Badge sizes
 */
enum class BadgeSize {
    Default,  // 10dp x 2dp padding, 12sp font
    Small     // 8dp x 2dp padding, 10sp font
}

/**
 * Get badge colors based on variant
 */
@Composable
private fun getBadgeColors(variant: BadgeVariant): Pair<Color, Color> {
    return when (variant) {
        BadgeVariant.Default -> Pair(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.primary
        )
        BadgeVariant.Secondary -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        BadgeVariant.Destructive -> Pair(
            DesperseTones.Destructive.copy(alpha = 0.1f),
            DesperseTones.Destructive
        )
        BadgeVariant.Success -> Pair(
            DesperseTones.Standard.copy(alpha = 0.2f),
            DesperseTones.Standard
        )
        BadgeVariant.Warning -> Pair(
            DesperseTones.Warning.copy(alpha = 0.2f),
            DesperseTones.Warning
        )
        BadgeVariant.Collectible -> Pair(
            DesperseTones.Collectible.copy(alpha = 0.15f),
            DesperseTones.Collectible
        )
        BadgeVariant.Edition -> Pair(
            DesperseTones.Edition.copy(alpha = 0.15f),
            DesperseTones.Edition
        )
        BadgeVariant.Outline -> Pair(
            Color.Transparent,
            MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Desperse Badge Component
 *
 * A small label used to display status, counts, or categories.
 *
 * @param text Badge text
 * @param variant Badge style variant
 * @param size Badge size
 * @param icon Optional leading icon
 * @param modifier Additional modifiers
 */
@Composable
fun DesperseBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Default,
    size: BadgeSize = BadgeSize.Default,
    icon: ImageVector? = null
) {
    val (backgroundColor, contentColor) = getBadgeColors(variant)

    val paddingHorizontal = when (size) {
        BadgeSize.Default -> 10.dp
        BadgeSize.Small -> 8.dp
    }
    val paddingVertical = 2.dp

    val textStyle = when (size) {
        BadgeSize.Default -> MaterialTheme.typography.bodySmall
        BadgeSize.Small -> MaterialTheme.typography.labelSmall
    }

    val baseModifier = modifier
        .clip(RoundedCornerShape(DesperseRadius.xs))
        .background(backgroundColor)
        .then(
            if (variant == BadgeVariant.Outline) {
                Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(DesperseRadius.xs)
                )
            } else {
                Modifier
            }
        )
        .padding(horizontal = paddingHorizontal, vertical = paddingVertical)

    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (size == BadgeSize.Small) 10.dp else 12.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = textStyle,
            color = contentColor
        )
    }
}

/**
 * Post type badge (Collectible, Edition, etc.)
 */
@Composable
fun PostTypeBadge(
    postType: String,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    if (postType == "post") return  // No badge for regular posts

    val variant = when (postType) {
        "collectible" -> BadgeVariant.Collectible
        "edition" -> BadgeVariant.Edition
        else -> BadgeVariant.Secondary
    }

    val text = when (postType) {
        "collectible" -> if (count != null) "Collectible · $count" else "Collectible"
        "edition" -> if (count != null) "Edition · $count" else "Edition"
        else -> postType.replaceFirstChar { it.uppercase() }
    }

    DesperseBadge(
        text = text,
        variant = variant,
        size = BadgeSize.Small,
        modifier = modifier
    )
}

/**
 * Notification badge (red dot or count)
 */
@Composable
fun NotificationBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return

    Box(
        modifier = modifier
            .size(if (count > 9) 18.dp else 16.dp)
            .clip(CircleShape)
            .background(DesperseTones.Destructive),
        contentAlignment = Alignment.Center
    ) {
        if (count <= 99) {
            Text(
                text = if (count > 9) "$count" else "$count",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        } else {
            Text(
                text = "99+",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

/**
 * Simple red dot indicator (for unread state)
 */
@Composable
fun UnreadDot(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 8.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(DesperseTones.Destructive)
    )
}
