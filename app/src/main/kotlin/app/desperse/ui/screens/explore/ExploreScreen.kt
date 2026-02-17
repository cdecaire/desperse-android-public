package app.desperse.ui.screens.explore

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.desperse.data.dto.response.SearchUser
import app.desperse.data.dto.response.SuggestedCreator
import app.desperse.data.model.Post
import app.desperse.ui.components.ButtonVariant
import app.desperse.ui.components.DesperseTextButton
import app.desperse.ui.components.GeometricAvatar
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.PostCard
import app.desperse.ui.components.ReportContentPreview
import app.desperse.ui.components.ReportSheet
import app.desperse.ui.theme.DesperseSpacing
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Report state
    var showReportSheet by remember { mutableStateOf(false) }
    var reportPostId by remember { mutableStateOf("") }
    var reportContentPreview by remember { mutableStateOf<ReportContentPreview?>(null) }

    // Load more when scrolling near the end
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (lastVisibleItem >= totalItems - 4 && !uiState.isLoadingMore && uiState.hasMore && !uiState.showSearchResults) {
                    viewModel.loadMore()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Explore",
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { scope.launch { listState.animateScrollToItem(0) } }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        // Report Sheet
        ReportSheet(
            open = showReportSheet,
            onDismiss = { showReportSheet = false },
            contentType = "post",
            contentPreview = reportContentPreview ?: ReportContentPreview(userName = ""),
            onSubmit = { reasons, details ->
                viewModel.createReport("post", reportPostId, reasons, details)
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onClear = viewModel::clearSearch,
                modifier = Modifier.padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm)
            )

            when {
                uiState.showSearchResults -> {
                    // Search Results
                    SearchResults(
                        users = uiState.searchUsers,
                        posts = uiState.searchPosts,
                        isSearching = uiState.isSearching,
                        onUserClick = onUserClick,
                        onPostClick = onPostClick,
                        onReportPost = { post ->
                            reportPostId = post.id
                            reportContentPreview = ReportContentPreview(
                                userName = post.user.displayName ?: post.user.slug,
                                userAvatarUrl = post.user.avatarUrl,
                                contentText = post.caption,
                                mediaUrl = post.coverUrl ?: post.mediaUrl
                            )
                            showReportSheet = true
                        }
                    )
                }

                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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

                else -> {
                    // Explore Content
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Suggested Creators Section
                        if (uiState.suggestedCreators.isNotEmpty()) {
                            item {
                                SuggestedCreatorsSection(
                                    creators = uiState.suggestedCreators,
                                    onCreatorClick = onUserClick
                                )
                            }

                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = DesperseSpacing.md),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Trending Section Header
                        item {
                            Text(
                                text = uiState.sectionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    horizontal = DesperseSpacing.lg,
                                    vertical = DesperseSpacing.sm
                                )
                            )
                        }

                        // Trending Posts
                        items(
                            items = uiState.trendingPosts,
                            key = { it.id }
                        ) { post ->
                            PostCard(
                                post = post,
                                onClick = { onPostClick(post.id) },
                                onUserClick = { onUserClick(post.user.slug) },
                                onLikeClick = { /* TODO: Handle like */ },
                                onCollectClick = { /* TODO: Handle collect */ },
                                onReport = {
                                    reportPostId = post.id
                                    reportContentPreview = ReportContentPreview(
                                        userName = post.user.displayName ?: post.user.slug,
                                        userAvatarUrl = post.user.avatarUrl,
                                        contentText = post.caption,
                                        mediaUrl = post.coverUrl ?: post.mediaUrl
                                    )
                                    showReportSheet = true
                                }
                            )
                        }

                        // Loading more indicator
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

                        // Empty state
                        if (uiState.trendingPosts.isEmpty() && !uiState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(DesperseSpacing.xl),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No posts to show",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = DesperseSpacing.md, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FaIcon(
            icon = FaIcons.MagnifyingGlass,
            size = 16.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            style = FaIconStyle.Solid
        )

        Spacer(modifier = Modifier.width(DesperseSpacing.sm))

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search users or posts...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        )

        if (query.isNotEmpty()) {
            Spacer(modifier = Modifier.width(DesperseSpacing.sm))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    .clickable { onClear() },
                contentAlignment = Alignment.Center
            ) {
                FaIcon(
                    icon = FaIcons.Xmark,
                    size = 10.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = FaIconStyle.Solid
                )
            }
        }
    }
}

@Composable
private fun SuggestedCreatorsSection(
    creators: List<SuggestedCreator>,
    onCreatorClick: (String) -> Unit
) {
    Column {
        Text(
            text = "Suggested Creators",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = DesperseSpacing.lg,
                vertical = DesperseSpacing.sm
            )
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = DesperseSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
        ) {
            items(
                items = creators,
                key = { it.id }
            ) { creator ->
                CreatorItem(
                    creator = creator,
                    onClick = { onCreatorClick(creator.usernameSlug) }
                )
            }
        }
    }
}

@Composable
private fun CreatorItem(
    creator: SuggestedCreator,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar with gradient ring (simplified - just a colored border)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (creator.avatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(creator.avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = creator.displayName ?: creator.usernameSlug,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    GeometricAvatar(
                        input = creator.usernameSlug,
                        size = 58.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = creator.displayName ?: creator.usernameSlug,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchResults(
    users: List<SearchUser>,
    posts: List<Post>,
    isSearching: Boolean,
    onUserClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onReportPost: (Post) -> Unit
) {
    if (isSearching) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesperseSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
        return
    }

    if (users.isEmpty() && posts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesperseSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No results found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Users section
        if (users.isNotEmpty()) {
            item {
                Text(
                    text = "People",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = DesperseSpacing.lg,
                        vertical = DesperseSpacing.sm
                    )
                )
            }

            items(
                items = users,
                key = { "user_${it.id}" }
            ) { user ->
                SearchUserItem(
                    user = user,
                    onClick = { onUserClick(user.usernameSlug) }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = DesperseSpacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Posts section
        if (posts.isNotEmpty()) {
            item {
                Text(
                    text = "Posts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = DesperseSpacing.lg,
                        vertical = DesperseSpacing.sm
                    )
                )
            }

            items(
                items = posts,
                key = { "post_${it.id}" }
            ) { post ->
                PostCard(
                    post = post,
                    onClick = { onPostClick(post.id) },
                    onUserClick = { onUserClick(post.user.slug) },
                    onLikeClick = { /* TODO: Handle like */ },
                    onCollectClick = { /* TODO: Handle collect */ },
                    onReport = { onReportPost(post) }
                )
            }
        }
    }
}

@Composable
private fun SearchUserItem(
    user: SearchUser,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = DesperseSpacing.lg, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (user.avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(user.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = user.displayName ?: user.usernameSlug,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                GeometricAvatar(
                    input = user.usernameSlug,
                    size = 44.dp
                )
            }
        }

        Spacer(modifier = Modifier.width(DesperseSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName ?: user.usernameSlug,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user.usernameSlug}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
