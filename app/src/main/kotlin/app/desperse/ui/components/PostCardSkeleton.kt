package app.desperse.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import app.desperse.ui.theme.DesperseColors
import app.desperse.ui.theme.DesperseComponentSpacing
import app.desperse.ui.theme.DesperseSpacing

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = listOf(
            DesperseColors.Zinc800,
            DesperseColors.Zinc700,
            DesperseColors.Zinc800
        ),
        start = Offset(translateAnim - 400f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

/**
 * Skeleton placeholder matching PostCard layout structure.
 * Shows shimmer animation while feed data loads.
 */
@Composable
fun PostCardSkeleton(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header: avatar + name/username + timestamp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesperseComponentSpacing.postCardHeaderPadding,
                    vertical = DesperseComponentSpacing.postCardHeaderPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(DesperseComponentSpacing.postCardAvatarSize)
                    .clip(CircleShape)
                    .background(brush)
            )
            Spacer(Modifier.width(DesperseComponentSpacing.postCardAvatarGap))
            Column {
                // Display name
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(Modifier.height(6.dp))
                // Username / timestamp
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }

        // Media placeholder (4:5 aspect ratio, matching feed fixed ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .background(brush)
        )

        // Actions row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesperseComponentSpacing.postCardContentPadding,
                    vertical = DesperseSpacing.sm
                ),
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.lg)
        ) {
            // Like button placeholder
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            // Comment button placeholder
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }

        // Caption lines
        Column(
            modifier = Modifier.padding(horizontal = DesperseComponentSpacing.postCardContentPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }

        // Bottom spacing between posts (matches PostCard)
        Spacer(modifier = Modifier.height(DesperseSpacing.lg))
    }
}
