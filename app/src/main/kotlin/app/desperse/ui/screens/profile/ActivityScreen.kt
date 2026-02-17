package app.desperse.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.desperse.data.dto.response.ActivityItem
import app.desperse.ui.components.ButtonVariant
import app.desperse.ui.components.DesperseTextButton
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseSpacing
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit = {},
    onBack: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Load data on first composition
    LaunchedEffect(Unit) {
        viewModel.load()
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
                title = { Text("Activity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        FaIcon(FaIcons.ArrowLeft, size = 20.dp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            uiState.error != null -> {
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

            uiState.activities.isEmpty() -> {
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
                            icon = FaIcons.History,
                            size = 48.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = FaIconStyle.Regular
                        )
                        Text(
                            text = "No activity yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(vertical = DesperseSpacing.sm)
                ) {
                    items(
                        items = uiState.activities,
                        key = { it.id }
                    ) { activity ->
                        ActivityListItem(
                            activity = activity,
                            onClick = {
                                if (activity.type == "tipped" && activity.tip != null) {
                                    onProfileClick(activity.tip.recipient.slug)
                                } else if (activity.post != null) {
                                    onPostClick(activity.post.id)
                                }
                            }
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

@Composable
private fun ActivityListItem(
    activity: ActivityItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isTip = activity.type == "tipped" && activity.tip != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isTip) {
            // Tip: show recipient avatar as thumbnail
            val avatarUrl = activity.tip!!.recipient.avatarUrl
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = activity.tip.recipient.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    FaIcon(
                        icon = FaIcons.User,
                        size = 20.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = FaIconStyle.Regular
                    )
                }
            }
        } else {
            // Post-based: show post thumbnail
            val thumbnailUrl = activity.post?.coverUrl ?: activity.post?.mediaUrl
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = activity.post?.caption,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        FaIcon(
                            icon = FaIcons.Image,
                            size = 20.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = FaIconStyle.Regular
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(DesperseSpacing.md))

        // Activity info
        Column(modifier = Modifier.weight(1f)) {
            if (isTip) {
                // Tip: show recipient name and amount
                val recipient = activity.tip!!.recipient
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = recipient.displayName ?: recipient.slug,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tipped",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF59E0B)
                    )
                }
                Text(
                    text = "${activity.tip.amount.toLong()} ${activity.tip.token}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (activity.post != null) {
                // Post-based: author info with activity type
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activity.post.user.avatarUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(activity.post.user.avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = activity.post.user.displayName ?: activity.post.user.slug,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = activity.post.user.displayName ?: activity.post.user.slug,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = getActivityLabel(activity.type),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!activity.post.caption.isNullOrBlank()) {
                    Text(
                        text = activity.post.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(DesperseSpacing.sm))

        // Activity type icon
        FaIcon(
            icon = getActivityIcon(activity.type),
            size = 16.dp,
            tint = if (isTip) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onBackground,
            style = FaIconStyle.Regular
        )
    }
}

private fun getActivityLabel(type: String): String {
    return when (type) {
        "post" -> "Posted"
        "like" -> "Liked"
        "commented" -> "Commented"
        "collected" -> "Collected"
        "bought" -> "Bought"
        "tipped" -> "Tipped"
        else -> type.replaceFirstChar { it.uppercase() }
    }
}

private fun getActivityIcon(type: String): String {
    return when (type) {
        "post" -> FaIcons.Plus
        "like" -> FaIcons.Heart
        "commented" -> FaIcons.Comment
        "collected" -> FaIcons.Gem
        "bought" -> FaIcons.Wallet
        "tipped" -> FaIcons.Coins
        else -> FaIcons.Clock
    }
}
