package app.desperse.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.desperse.data.dto.response.NotificationItem
import app.desperse.ui.components.AvatarSize
import app.desperse.ui.components.ButtonVariant
import app.desperse.ui.components.DesperseAvatar
import app.desperse.ui.components.DesperseTextButton
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.media.ImageContext
import app.desperse.ui.components.media.ImageOptimization
import app.desperse.ui.theme.DesperseSpacing
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }

    // Clear all confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all notifications?") },
            text = { Text("This will clear all your notifications. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAll()
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Load more when scrolling near the end
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (lastVisibleItem >= totalItems - 6 && !uiState.isLoadingMore && uiState.hasMore) {
                    viewModel.loadMore()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { scope.launch { listState.animateScrollToItem(0) } }
                    )
                },
                actions = {
                    if (uiState.notifications.isNotEmpty()) {
                        // Mark all as read button
                        if (uiState.notifications.any { !it.isRead }) {
                            IconButton(
                                onClick = { viewModel.markAllAsRead() },
                                enabled = !uiState.isMarkingAllRead
                            ) {
                                FaIcon(
                                    icon = FaIcons.Check,
                                    size = 18.dp,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Clear all button
                        IconButton(
                            onClick = { showClearDialog = true },
                            enabled = !uiState.isLoading
                        ) {
                            FaIcon(
                                icon = FaIcons.MessageCheck,
                                size = 18.dp,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading && uiState.notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            uiState.error != null && uiState.notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.lg)
                    ) {
                        FaIcon(
                            icon = FaIcons.TriangleExclamation,
                            size = 48.dp,
                            tint = MaterialTheme.colorScheme.error,
                            style = FaIconStyle.Solid
                        )
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        DesperseTextButton(
                            text = "Retry",
                            onClick = { viewModel.load() },
                            variant = ButtonVariant.Default
                        )
                    }
                }
            }

            uiState.notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
                    ) {
                        FaIcon(
                            icon = FaIcons.Bell,
                            size = 48.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = FaIconStyle.Regular
                        )
                        Text(
                            text = "No notifications yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "When someone interacts with your posts, you'll see it here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = DesperseSpacing.sm)
                    ) {
                        items(
                            items = uiState.notifications,
                            key = { it.id }
                        ) { notification ->
                            NotificationListItem(
                                notification = notification,
                                onPostClick = onPostClick,
                                onUserClick = onUserClick
                            )
                        }

                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(DesperseSpacing.lg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationListItem(
    notification: NotificationItem,
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    val context = LocalContext.current

    // Determine what to navigate to when clicking the notification
    val onClick = remember(notification) {
        {
            when (notification.type) {
                "follow" -> onUserClick(notification.actor.usernameSlug)
                "comment", "mention" -> {
                    // Navigate to the post (either from referenceId or reference.postId)
                    val postId = notification.reference?.postId ?: notification.referenceId
                    if (postId != null) onPostClick(postId)
                }
                else -> {
                    // Like, collect, purchase - navigate to the post
                    notification.referenceId?.let { onPostClick(it) }
                }
            }
        }
    }

    // Get thumbnail URL for post-related notifications
    val thumbnailUrl = remember(notification) {
        if (notification.type != "follow") {
            notification.reference?.coverUrl ?: notification.reference?.mediaUrl
        } else null
    }

    val optimizedThumbnail = remember(thumbnailUrl) {
        thumbnailUrl?.let { ImageOptimization.getOptimizedUrlForContext(it, ImageContext.AVATAR) }
    }

    // Unread indicator background
    val backgroundColor = if (notification.isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        // Actor avatar
        DesperseAvatar(
            imageUrl = notification.actor.avatarUrl,
            contentDescription = notification.actor.displayName ?: notification.actor.usernameSlug,
            identityInput = notification.actor.usernameSlug,
            size = AvatarSize.Medium,
            modifier = Modifier.clickable { onUserClick(notification.actor.usernameSlug) }
        )

        Spacer(modifier = Modifier.width(DesperseSpacing.md))

        // Notification content
        Column(modifier = Modifier.weight(1f)) {
            // Actor name + action text
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(notification.actor.displayName ?: notification.actor.usernameSlug)
                    }
                    append(" ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append(getNotificationText(notification.type, notification.referenceType))
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Comment/mention preview
            if ((notification.type == "comment" || notification.type == "mention") &&
                !notification.reference?.content.isNullOrBlank()
            ) {
                Text(
                    text = "\"${notification.reference?.content}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Timestamp
            Text(
                text = formatRelativeTime(notification.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Post thumbnail (for non-follow notifications)
        if (optimizedThumbnail != null) {
            Spacer(modifier = Modifier.width(DesperseSpacing.sm))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(optimizedThumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * Get human-readable notification action text
 */
private fun getNotificationText(type: String, referenceType: String?): String {
    return when (type) {
        "follow" -> "started following you"
        "like" -> "liked your post"
        "comment" -> "commented on your post"
        "collect" -> "collected your post"
        "purchase" -> "bought your edition"
        "mention" -> if (referenceType == "comment") {
            "mentioned you in a comment"
        } else {
            "mentioned you in a post"
        }
        else -> "interacted with you"
    }
}

/**
 * Format timestamp as relative time (e.g., "2m", "3h", "5d")
 */
private fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val now = Instant.now()
        val duration = Duration.between(instant, now)

        when {
            duration.seconds < 60 -> "now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m"
            duration.toHours() < 24 -> "${duration.toHours()}h"
            duration.toDays() < 7 -> "${duration.toDays()}d"
            duration.toDays() < 30 -> "${duration.toDays() / 7}w"
            else -> {
                // Format as date for older notifications
                val formatter = DateTimeFormatter.ofPattern("MMM d")
                val zonedDateTime = instant.atZone(java.time.ZoneId.systemDefault())
                formatter.format(zonedDateTime)
            }
        }
    } catch (e: DateTimeParseException) {
        ""
    }
}
