package app.desperse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.desperse.data.dto.response.Comment
import app.desperse.ui.components.media.ImageContext
import app.desperse.ui.components.media.ImageOptimization
import app.desperse.ui.theme.DesperseSpacing
import coil.compose.AsyncImage
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * A comment item that can be swiped left to reveal actions:
 * - Own comments: swipe to delete
 * - Other users' comments: swipe to report
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableCommentItem(
    comment: Comment,
    isOwnComment: Boolean,
    isDeleting: Boolean,
    onUserClick: () -> Unit,
    onMentionClick: (String) -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swiped from end to start (left swipe)
                    if (isOwnComment) {
                        onDelete()
                    } else {
                        onReport()
                    }
                    false // Don't dismiss, let the callback handle it
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !isDeleting,
        backgroundContent = {
            // Action background (revealed on swipe)
            val backgroundColor by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (isOwnComment) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.errorContainer
                    }
                    else -> Color.Transparent
                },
                label = "backgroundColor"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = DesperseSpacing.lg),
                contentAlignment = Alignment.CenterEnd
            ) {
                FaIcon(
                    icon = if (isOwnComment) FaIcons.Trash else FaIcons.Flag,
                    size = 20.dp,
                    tint = if (isOwnComment) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }
    ) {
        CommentContent(
            comment = comment,
            onUserClick = onUserClick,
            onMentionClick = onMentionClick,
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        )
    }
}

@Composable
private fun CommentContent(
    comment: Comment,
    onUserClick: () -> Unit,
    onMentionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val optimizedAvatarUrl = remember(comment.user.avatarUrl) {
        comment.user.avatarUrl?.let {
            ImageOptimization.getOptimizedUrlForContext(it, ImageContext.AVATAR)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.md, vertical = DesperseSpacing.sm)
    ) {
        // Avatar
        AsyncImage(
            model = optimizedAvatarUrl,
            contentDescription = comment.user.displayName ?: comment.user.slug,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onUserClick),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(DesperseSpacing.sm))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = comment.user.displayName ?: comment.user.slug,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onUserClick)
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatRelativeTime(comment.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            MentionText(
                text = comment.content,
                onMentionClick = onMentionClick,
                style = MaterialTheme.typography.bodyMedium,
                textColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Format a timestamp as relative time (e.g., "2m", "3h", "1d")
 */
private fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val now = Instant.now()

        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours = ChronoUnit.HOURS.between(instant, now)
        val days = ChronoUnit.DAYS.between(instant, now)

        when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            days < 365 -> "${days / 7}w"
            else -> "${days / 365}y"
        }
    } catch (e: Exception) {
        ""
    }
}
