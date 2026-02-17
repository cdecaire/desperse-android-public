package app.desperse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import app.desperse.ui.theme.DesperseSpacing

/**
 * Skeleton placeholder matching ProfileScreen layout.
 * Shows shimmer animation while profile data loads.
 */
@Composable
fun ProfileSkeleton(
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Banner (160dp, matching ProfileHeader)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(brush)
        )

        // Avatar + action icons row (offset to overlap banner)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-40).dp)
                .padding(horizontal = DesperseSpacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Avatar circle (80dp with border)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(brush)
            )
            // Action icon placeholders
            Row(horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
            }
        }

        // Name + handle + bio (pulled up since avatar overlaps)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-28).dp)
                .padding(horizontal = DesperseSpacing.lg)
        ) {
            // Display name
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(Modifier.height(8.dp))
            // @handle
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(Modifier.height(12.dp))
            // Bio line 1
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(Modifier.height(6.dp))
            // Bio line 2
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(Modifier.height(16.dp))

            // Stats row (followers, following, collectors)
            Row(horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xl)) {
                repeat(3) {
                    Column {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                    }
                }
            }
        }

        // Tabs placeholder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-20).dp)
                .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xxl)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }

        // Grid placeholder (3x2 grid of squares)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(brush)
                        )
                    }
                }
            }
        }
    }
}
