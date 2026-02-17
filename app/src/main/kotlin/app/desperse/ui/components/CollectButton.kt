package app.desperse.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.desperse.data.model.CollectState
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.DesperseTones

/**
 * Collect button for free collectibles (cNFTs) - COMPACT FORMAT
 *
 * Matches web app design: icon + count only (like Like/Comment buttons)
 * Icon: gem (solid when collected, regular when not)
 *
 * States:
 * - Idle: Regular gem icon + count
 * - Preparing/Confirming: Spinner + count
 * - Success/Collected: Solid gem icon (tinted) + count
 * - Failed: Retry icon with error color
 */
@Composable
fun CollectButton(
    collectCount: Int,
    isCollected: Boolean,
    collectState: CollectState = CollectState.Idle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toneColor = DesperseTones.Collectible

    // Determine display state - isSuccess takes priority over isFailed
    val isInProgress = collectState is CollectState.Preparing || collectState is CollectState.Confirming
    val isSuccess = collectState is CollectState.Success || isCollected
    val isFailed = collectState is CollectState.Failed && !isSuccess

    // Whether button is clickable
    val isClickable = !isInProgress && !isSuccess &&
        (!isFailed || (collectState as? CollectState.Failed)?.canRetry == true)

    // Color: toneColor when collected, error for failed, muted otherwise
    val contentColor = when {
        isSuccess -> toneColor
        isFailed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Icon based on state
    val icon = when {
        isFailed -> FaIcons.ArrowsRotate
        else -> FaIcons.Gem
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isClickable) { onClick() }
            .padding(DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
    ) {
        if (isInProgress) {
            // Show spinner when in progress
            CircularProgressIndicator(
                modifier = Modifier.size(DesperseSizes.iconMd),
                color = toneColor,
                strokeWidth = 2.dp
            )
        } else {
            FaIcon(
                icon = icon,
                size = DesperseSizes.iconMd,
                tint = contentColor,
                style = if (isSuccess) FaIconStyle.Solid else FaIconStyle.Regular,
                contentDescription = when {
                    isSuccess -> "Collected"
                    isFailed -> "Retry"
                    else -> "Collect"
                }
            )
        }

        // Always show count (like Like/Comment buttons)
        if (collectCount > 0 || isSuccess) {
            Text(
                text = formatCount(collectCount),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

/**
 * Format count (1.2k, 1.5M, etc.)
 */
private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
        count >= 1_000 -> "%.1fk".format(count / 1_000.0)
        else -> count.toString()
    }
}
