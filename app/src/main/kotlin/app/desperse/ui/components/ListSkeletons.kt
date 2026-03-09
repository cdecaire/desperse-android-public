package app.desperse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing

/**
 * Skeleton for notification list items.
 * Layout: avatar (40dp) + text lines + optional thumbnail (48dp)
 */
@Composable
fun NotificationItemSkeleton(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(DesperseSizes.avatarMd)
                .clip(CircleShape)
                .background(brush)
        )

        Spacer(Modifier.width(DesperseSpacing.md))

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            // Actor name + action
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .background(brush)
            )
            Spacer(Modifier.height(6.dp))
            // Timestamp
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .background(brush)
            )
        }

        Spacer(Modifier.width(DesperseSpacing.sm))

        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(DesperseRadius.sm))
                .background(brush)
        )
    }
}

/**
 * Skeleton for activity list items.
 * Layout: thumbnail (56dp) + text lines + icon
 */
@Composable
fun ActivityItemSkeleton(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(DesperseRadius.sm))
                .background(brush)
        )

        Spacer(Modifier.width(DesperseSpacing.md))

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .background(brush)
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .background(brush)
            )
        }

        Spacer(Modifier.width(DesperseSpacing.sm))

        // Icon placeholder
        Box(
            modifier = Modifier
                .size(DesperseSizes.iconSm)
                .clip(CircleShape)
                .background(brush)
        )
    }
}

/**
 * Skeleton for user list items (followers, following, collectors, search results).
 * Layout: avatar (48dp) + text lines + button
 */
@Composable
fun UserItemSkeleton(
    brush: Brush,
    showButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(DesperseSizes.avatarLg)
                .clip(CircleShape)
                .background(brush)
        )

        Spacer(Modifier.width(DesperseSpacing.md))

        // Name + username
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .background(brush)
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .background(brush)
            )
        }

        if (showButton) {
            Spacer(Modifier.width(DesperseSpacing.sm))
            // Follow button placeholder
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(DesperseSizes.buttonDefault)
                    .clip(CircleShape)
                    .background(brush)
            )
        }
    }
}

/**
 * Skeleton for thread/conversation list items.
 * Layout: avatar (large) + name/preview lines
 */
@Composable
fun ThreadItemSkeleton(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(DesperseSizes.avatarLg)
                .clip(CircleShape)
                .background(brush)
        )

        Spacer(Modifier.width(DesperseSpacing.md))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
        ) {
            // Name + time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(DesperseRadius.xs))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(DesperseRadius.xs))
                        .background(brush)
                )
            }
            // Message preview
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .background(brush)
            )
        }
    }
}

/**
 * Skeleton for message bubbles in a conversation.
 * Alternates between sent (right-aligned) and received (left-aligned) bubbles.
 */
@Composable
fun MessageBubbleSkeleton(
    brush: Brush,
    isSent: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.xs),
        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isSent) 0.6f else 0.7f)
                .height(if (isSent) 36.dp else 48.dp)
                .clip(RoundedCornerShape(DesperseRadius.lg))
                .background(brush)
        )
    }
}

/**
 * Skeleton for explore search results (user items).
 */
@Composable
fun SearchResultSkeleton(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(brush)
        )

        Spacer(Modifier.width(DesperseSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .background(brush)
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .background(brush)
            )
        }
    }
}

/**
 * Skeleton for storage credits page content.
 * Shows placeholder for balance card + details section.
 */
@Composable
fun StorageCreditsSkeleton(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(DesperseSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.lg)
    ) {
        // Balance card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(DesperseRadius.md))
                .background(brush)
        )

        // Section header
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(DesperseRadius.xs))
                .background(brush)
        )

        // Detail rows
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(DesperseRadius.xs))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(DesperseRadius.xs))
                        .background(brush)
                )
            }
        }

        // Button placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DesperseSizes.buttonCta)
                .clip(CircleShape)
                .background(brush)
        )
    }
}
