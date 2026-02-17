package app.desperse.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.desperse.data.dto.response.FollowUser
import app.desperse.ui.components.ButtonVariant
import app.desperse.ui.components.DesperseTextButton
import app.desperse.ui.components.GeometricAvatar
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseSpacing
import coil.compose.AsyncImage
import coil.request.ImageRequest

enum class FollowListType {
    Followers,
    Following,
    Collectors
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    slug: String,
    listType: FollowListType,
    onUserClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: FollowListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Load data on first composition
    LaunchedEffect(slug, listType) {
        viewModel.load(slug, listType)
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

    val title = when (listType) {
        FollowListType.Followers -> "Followers"
        FollowListType.Following -> "Following"
        FollowListType.Collectors -> "Collectors"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                            onClick = { viewModel.load(slug, listType) },
                            variant = ButtonVariant.Default
                        )
                    }
                }
            }

            uiState.users.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyMessage = when (listType) {
                        FollowListType.Followers -> "No followers yet"
                        FollowListType.Following -> "Not following anyone"
                        FollowListType.Collectors -> "No collectors yet"
                    }
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        items = uiState.users,
                        key = { it.id }
                    ) { user ->
                        FollowUserItem(
                            user = user,
                            isCurrentUser = user.id == viewModel.currentUserId,
                            onUserClick = { onUserClick(user.slug) },
                            onFollowClick = { viewModel.toggleFollow(user.id) },
                            isFollowLoading = uiState.followLoadingIds.contains(user.id)
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
private fun FollowUserItem(
    user: FollowUser,
    isCurrentUser: Boolean = false,
    onUserClick: () -> Unit,
    onFollowClick: () -> Unit,
    isFollowLoading: Boolean
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick() }
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (user.avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(user.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = user.displayName ?: user.slug,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                GeometricAvatar(
                    input = user.slug,
                    size = 48.dp
                )
            }
        }

        Spacer(modifier = Modifier.width(DesperseSpacing.md))

        // User info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName ?: user.slug,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user.slug}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isCurrentUser) {
            Spacer(modifier = Modifier.width(DesperseSpacing.sm))

            // Follow button
            DesperseTextButton(
                text = if (user.isFollowing) "Following" else "Follow",
                onClick = onFollowClick,
                variant = if (user.isFollowing) ButtonVariant.Secondary else ButtonVariant.Default,
                enabled = !isFollowLoading
            )
        }
    }
}
