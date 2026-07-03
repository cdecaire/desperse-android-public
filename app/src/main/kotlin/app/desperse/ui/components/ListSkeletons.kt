package app.desperse.ui.components

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
import androidx.compose.ui.unit.dp
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing

@Composable
fun NotificationItemSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(DesperseSizes.avatarMd)
                .clip(CircleShape)
                .shimmer()
        )

        Spacer(Modifier.width(DesperseSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .shimmer()
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .shimmer()
            )
        }

        Spacer(Modifier.width(DesperseSpacing.sm))

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(DesperseRadius.sm))
                .shimmer()
        )
    }
}

@Composable
fun ActivityItemSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(DesperseRadius.sm))
                .shimmer()
        )

        Spacer(Modifier.width(DesperseSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .shimmer()
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .shimmer()
            )
        }

        Spacer(Modifier.width(DesperseSpacing.sm))

        Box(
            modifier = Modifier
                .size(DesperseSizes.iconSm)
                .clip(CircleShape)
                .shimmer()
        )
    }
}

@Composable
fun UserItemSkeleton(
    showButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(DesperseSizes.avatarLg)
                .clip(CircleShape)
                .shimmer()
        )

        Spacer(Modifier.width(DesperseSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .shimmer()
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .shimmer()
            )
        }

        if (showButton) {
            Spacer(Modifier.width(DesperseSpacing.sm))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(DesperseSizes.buttonDefault)
                    .clip(CircleShape)
                    .shimmer()
            )
        }
    }
}

@Composable
fun ThreadItemSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(DesperseSizes.avatarLg)
                .clip(CircleShape)
                .shimmer()
        )

        Spacer(Modifier.width(DesperseSpacing.md))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(DesperseRadius.xs))
                        .shimmer()
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(DesperseRadius.xs))
                        .shimmer()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .shimmer()
            )
        }
    }
}

@Composable
fun MessageBubbleSkeleton(
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
                .shimmer()
        )
    }
}

@Composable
fun SearchResultSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .shimmer()
        )

        Spacer(Modifier.width(DesperseSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .shimmer()
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(DesperseRadius.xs))
                    .shimmer()
            )
        }
    }
}

@Composable
fun StorageCreditsSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(DesperseSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.lg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(DesperseRadius.md))
                .shimmer()
        )

        Box(
            modifier = Modifier
                .width(140.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(DesperseRadius.xs))
                .shimmer()
        )

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
                        .shimmer()
                )
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(DesperseRadius.xs))
                        .shimmer()
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DesperseSizes.buttonCta)
                .clip(CircleShape)
                .shimmer()
        )
    }
}
