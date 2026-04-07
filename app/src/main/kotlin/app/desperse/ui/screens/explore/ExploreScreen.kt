package app.desperse.ui.screens.explore

import app.desperse.ui.components.ErrorState
import app.desperse.ui.components.LoadingMoreIndicator
import app.desperse.ui.components.PostCardSkeleton
import app.desperse.ui.components.SearchResultSkeleton
import app.desperse.ui.components.rememberShimmerBrush
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.desperse.data.dto.response.SearchUser
import app.desperse.data.dto.response.SuggestedCreator
import app.desperse.data.model.CollectState
import app.desperse.data.model.Post
import app.desperse.ui.components.ButtonVariant
import app.desperse.ui.components.CommentSheet
import app.desperse.ui.components.DesperseTextButton
import app.desperse.ui.components.GeometricAvatar
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.PostCard
import app.desperse.ui.components.ReportContentPreview
import app.desperse.ui.components.ReportSheet
import app.desperse.ui.components.media.ImageContext
import app.desperse.ui.components.media.ImageOptimization
import app.desperse.ui.screens.feed.CommentSheetViewModel
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
    val collectStates by viewModel.collectStates.collectAsState()
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullToRefreshState()

    // Report state
    var showReportSheet by remember { mutableStateOf(false) }
    var reportPostId by remember { mutableStateOf("") }
    var reportContentPreview by remember { mutableStateOf<ReportContentPreview?>(null) }

    // Comment sheet state
    var showCommentSheet by remember { mutableStateOf(false) }
    var reportContentType by remember { mutableStateOf("post") }
    val commentSheetViewModel: CommentSheetViewModel = hiltViewModel()

    // Load more when scrolling near the end (grid)
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (lastVisibleItem >= totalItems - 6 && !uiState.isLoadingMore && uiState.hasMore && !uiState.showSearchResults) {
                    viewModel.loadMore()
                }
            }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Explore") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        // Report Sheet
        ReportSheet(
            open = showReportSheet,
            onDismiss = { showReportSheet = false },
            contentType = reportContentType,
            contentPreview = reportContentPreview ?: ReportContentPreview(userName = ""),
            onSubmit = { reasons, details ->
                viewModel.createReport(reportContentType, reportPostId, reasons, details)
            }
        )

        // Comment Sheet
        CommentSheet(
            isOpen = showCommentSheet,
            onDismiss = {
                showCommentSheet = false
                commentSheetViewModel.clearState()
            },
            onUserClick = { slug ->
                showCommentSheet = false
                onUserClick(slug)
            },
            onReportComment = { comment ->
                showCommentSheet = false
                reportContentType = "comment"
                reportPostId = comment.id
                reportContentPreview = ReportContentPreview(
                    userName = comment.user.displayName ?: comment.user.slug,
                    userAvatarUrl = comment.user.avatarUrl,
                    contentText = comment.content
                )
                showReportSheet = true
            },
            viewModel = commentSheetViewModel
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

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
            when {
                uiState.showSearchResults -> {
                    // Search Results
                    SearchResults(
                        users = uiState.searchUsers,
                        posts = uiState.searchPosts,
                        collectStates = collectStates,
                        isSearching = uiState.isSearching,
                        onUserClick = onUserClick,
                        onPostClick = onPostClick,
                        onLikeClick = viewModel::likePost,
                        onCollectClick = viewModel::collectPost,
                        onCommentClick = { postId, commentCount ->
                            commentSheetViewModel.openForPost(postId, commentCount)
                            showCommentSheet = true
                        },
                        onReportPost = { post ->
                            reportContentType = "post"
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
                    val brush = rememberShimmerBrush()
                    // Grid skeleton — 3 columns of placeholder tiles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = DesperseSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.8f)
                                    .background(brush, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.8f)
                                    .background(brush, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.load() }
                    )
                }

                else -> {
                    // Explore Content — 3-column grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Suggested Creators Section (full width)
                        if (uiState.suggestedCreators.isNotEmpty()) {
                            item(span = { GridItemSpan(3) }) {
                                SuggestedCreatorsSection(
                                    creators = uiState.suggestedCreators,
                                    onCreatorClick = onUserClick
                                )
                            }

                            item(span = { GridItemSpan(3) }) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = DesperseSpacing.md),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Posts Grid
                        items(
                            items = uiState.trendingPosts,
                            key = { it.id },
                            contentType = { "grid_post" }
                        ) { post ->
                            ExploreGridItem(
                                post = post,
                                onClick = { onPostClick(post.id) }
                            )
                        }

                        if (uiState.isLoadingMore) {
                            item(span = { GridItemSpan(3) }) { LoadingMoreIndicator() }
                        }

                        // Empty state
                        if (uiState.trendingPosts.isEmpty() && !uiState.isLoading) {
                            item(span = { GridItemSpan(3) }) {
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
            } // PullToRefreshBox
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
                val onClickStable = remember(creator.usernameSlug) { { onCreatorClick(creator.usernameSlug) } }
                CreatorItem(
                    creator = creator,
                    onClick = onClickStable
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
                    val optimizedUrl = remember(creator.avatarUrl) {
                        ImageOptimization.getOptimizedUrlForContext(creator.avatarUrl, ImageContext.AVATAR)
                    }
                    val imageRequest = remember(optimizedUrl) {
                        ImageRequest.Builder(context)
                            .data(optimizedUrl)
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
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
    collectStates: Map<String, CollectState>,
    isSearching: Boolean,
    onUserClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onCollectClick: (String) -> Unit,
    onCommentClick: (String, Int) -> Unit,
    onReportPost: (Post) -> Unit
) {
    if (isSearching) {
        val brush = rememberShimmerBrush()
        Column(modifier = Modifier.fillMaxWidth()) {
            repeat(4) {
                SearchResultSkeleton(brush = brush)
            }
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
                key = { "user_${it.id}" },
                contentType = { "search_user" }
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
                key = { "post_${it.id}" },
                contentType = { "search_post" }
            ) { post ->
                val onClickStable = remember(post.id) { { onPostClick(post.id) } }
                val onUserClickStable = remember(post.user.slug) { { onUserClick(post.user.slug) } }
                val onLikeClickStable = remember(post.id) { { onLikeClick(post.id) } }
                val onCollectClickStable = remember(post.id) { { onCollectClick(post.id) } }
                val onCommentClickStable = remember(post.id, post.commentCount) {
                    { onCommentClick(post.id, post.commentCount) }
                }
                val onReportStable = remember(post) { { onReportPost(post) } }

                val collectState by remember(post.id) {
                    derivedStateOf { collectStates[post.id] ?: CollectState.Idle }
                }

                PostCard(
                    post = post,
                    onClick = onClickStable,
                    onUserClick = onUserClickStable,
                    onMentionClick = onUserClick,
                    onLikeClick = onLikeClickStable,
                    onCommentClick = onCommentClickStable,
                    onCollectClick = onCollectClickStable,
                    onReport = onReportStable,
                    collectState = collectState
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
                val optimizedUrl = remember(user.avatarUrl) {
                    ImageOptimization.getOptimizedUrlForContext(user.avatarUrl, ImageContext.AVATAR)
                }
                val imageRequest = remember(optimizedUrl) {
                    ImageRequest.Builder(context)
                        .data(optimizedUrl)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
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

@Composable
private fun ExploreGridItem(
    post: Post,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val isVideo = post.mediaUrl?.let { url ->
        val extension = url.substringAfterLast('.').substringBefore('?').lowercase()
        extension in listOf("mp4", "webm", "mov")
    } ?: false

    val imageUrl = post.coverUrl ?: post.mediaUrl

    val thumbnailUrl = remember(imageUrl, isVideo) {
        if (imageUrl == null) null
        else if (isVideo && post.coverUrl == null) imageUrl
        else ImageOptimization.getOptimizedUrlForContext(imageUrl, ImageContext.PROFILE_GRID)
    }

    Box(
        modifier = Modifier
            .aspectRatio(0.8f) // 4:5 to match feed
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = remember(thumbnailUrl) {
                    ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .crossfade(true)
                        .build()
                },
                contentDescription = post.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low
            )
        }

        // Video badge (top right)
        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                FaIcon(
                    icon = FaIcons.Play,
                    size = 8.dp,
                    tint = Color.White,
                    style = FaIconStyle.Solid
                )
            }
        }
        // Collectible/Edition badge
        else if (post.type == "collectible" || post.type == "edition") {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                FaIcon(
                    icon = if (post.type == "collectible") FaIcons.Gem else FaIcons.LayerGroup,
                    size = 10.dp,
                    tint = Color.White,
                    style = FaIconStyle.Solid
                )
            }
        }
    }
}
