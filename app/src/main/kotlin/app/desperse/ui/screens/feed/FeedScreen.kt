package app.desperse.ui.screens.feed

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import app.desperse.data.NotificationCounters
import app.desperse.ui.components.NewPostsToast
import app.desperse.ui.components.NotificationBadge
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.desperse.R
import app.desperse.data.model.CollectState
import app.desperse.data.model.PurchaseState
import androidx.hilt.navigation.compose.hiltViewModel
import android.app.Activity
import app.desperse.ui.components.ButtonVariant
import app.desperse.ui.components.DesperseFaIconButton
import app.desperse.ui.components.DesperseTextButton
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.CommentSheet
import app.desperse.ui.components.InstalledWallet
import app.desperse.ui.components.PostCard
import app.desperse.ui.components.PostCardSkeleton
import app.desperse.ui.components.ReportContentPreview
import app.desperse.ui.components.ReportSheet
import app.desperse.ui.components.WalletPickerSheet
import app.desperse.ui.components.WalletSheet
import app.desperse.ui.components.rememberShimmerBrush
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing

/**
 * Feed Screen
 *
 * Main feed showing posts in For You or Following tabs.
 * Uses Material 3 MediumTopAppBar with proper scroll behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onNotificationsClick: () -> Unit = {},
    onEditPost: (String) -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val collectStates by viewModel.collectStates.collectAsState()
    val purchaseStates by viewModel.purchaseStates.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val notificationCounters by viewModel.notificationCounters.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val activity = androidx.compose.ui.platform.LocalContext.current as Activity

    // Per-tab lazy list states to preserve scroll position
    val forYouListState = rememberLazyListState()
    val followingListState = rememberLazyListState()
    val listState = if (selectedTab == "for-you") forYouListState else followingListState

    // Show new posts toast when scrolled down and new posts exist
    val showNewPostsToast by remember {
        derivedStateOf {
            val hasScrolled = listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > 200
            val hasNewPosts = when (selectedTab) {
                "for-you" -> notificationCounters.forYouNewPostsCount > 0
                "following" -> notificationCounters.followingNewPostsCount > 0
                else -> false
            }
            hasScrolled && hasNewPosts
        }
    }

    // Infinite scroll: load more when near the end of the list
    LaunchedEffect(listState) {
        snapshotFlow {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems to lastVisibleIndex
        }.collect { (totalItems, lastVisibleIndex) ->
            if (totalItems > 0 && lastVisibleIndex >= totalItems - 6) {
                viewModel.loadMore()
            }
        }
    }

    // Get creators for current tab
    val currentTabCreators = when (selectedTab) {
        "for-you" -> notificationCounters.forYouCreators
        "following" -> notificationCounters.followingCreators
        else -> emptyList()
    }

    // Wallet sheet state
    var showWalletSheet by remember { mutableStateOf(false) }

    // Report state
    var showReportSheet by remember { mutableStateOf(false) }
    var reportPostId by remember { mutableStateOf("") }
    var reportContentPreview by remember { mutableStateOf<ReportContentPreview?>(null) }

    // Delete confirmation state
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deletePostId by remember { mutableStateOf("") }

    // Block confirmation state
    var showBlockConfirmation by remember { mutableStateOf(false) }
    var blockUserId by remember { mutableStateOf("") }
    var blockUserDisplayName by remember { mutableStateOf("") }

    // Comment sheet state
    var showCommentSheet by remember { mutableStateOf(false) }
    var reportContentType by remember { mutableStateOf("post") }
    val commentSheetViewModel: CommentSheetViewModel = hiltViewModel()

    // Lifecycle observer for periodic refresh
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onScreenVisible()
                Lifecycle.Event.ON_PAUSE -> viewModel.onScreenHidden()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Use provided scroll behavior or create one locally
    val effectiveScrollBehavior = scrollBehavior ?: TopAppBarDefaults.enterAlwaysScrollBehavior()

    // CRITICAL: nestedScroll must be on Scaffold, not on content
    Scaffold(
        modifier = Modifier.nestedScroll(effectiveScrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FeedTopBar(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.switchTab(it) },
                onWalletClick = { showWalletSheet = true },
                onNotificationsClick = onNotificationsClick,
                hasUnreadNotifications = notificationCounters.unreadNotifications > 0,
                onTitleClick = { scope.launch { listState.animateScrollToItem(0) } },
                scrollBehavior = effectiveScrollBehavior,
                forYouNewCount = notificationCounters.forYouNewPostsCount,
                followingNewCount = notificationCounters.followingNewPostsCount
            )
        }
    ) { innerPadding ->
        // Wallet Sheet
        WalletSheet(
            isOpen = showWalletSheet,
            onDismiss = { showWalletSheet = false }
        )

        // Wallet Picker Sheet (shown when external wallet package is unknown for purchases)
        if (uiState.showWalletPicker) {
            WalletPickerSheet(
                wallets = uiState.installedWallets.map { mwaWallet ->
                    InstalledWallet(
                        packageName = mwaWallet.packageName,
                        displayName = mwaWallet.displayName,
                        walletClientType = mwaWallet.walletClientType
                    )
                },
                onWalletSelected = { wallet ->
                    viewModel.onWalletSelectedForTransaction(wallet.packageName)
                },
                onDismiss = { viewModel.dismissWalletPicker() }
            )
        }

        // Report Sheet
        ReportSheet(
            open = showReportSheet,
            onDismiss = {
                showReportSheet = false
                reportContentType = "post"
            },
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

        // Block confirmation dialog
        if (showBlockConfirmation) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showBlockConfirmation = false },
                title = { Text("Block @$blockUserDisplayName?") },
                text = { Text("They won't be able to find your profile or posts, and you won't see theirs. They won't be notified.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.blockUser(blockUserId, blockUserDisplayName)
                            showBlockConfirmation = false
                        },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Block") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showBlockConfirmation = false }
                    ) { Text("Cancel") }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Delete confirmation dialog
        if (showDeleteConfirmation) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Post") },
                text = { Text("Are you sure you want to delete this post? This action cannot be undone.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.deletePost(deletePostId)
                            showDeleteConfirmation = false
                        },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showDeleteConfirmation = false }
                    ) { Text("Cancel") }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Content MUST use innerPadding - this is how Scaffold coordinates with TopAppBar
        // Apply top padding to PullToRefreshBox so the indicator appears below the header
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when {
                uiState.isLoading && uiState.posts.isEmpty() -> {
                    val shimmerBrush = rememberShimmerBrush()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = innerPadding.calculateBottomPadding() + DesperseSizes.bottomNavHeight + DesperseSpacing.lg
                        ),
                        userScrollEnabled = false
                    ) {
                        items(3) {
                            PostCardSkeleton(brush = shimmerBrush)
                        }
                    }
                }

                uiState.error != null && uiState.posts.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        FeedErrorState(
                            message = uiState.error!!,
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }

                uiState.posts.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        FeedEmptyState(
                            isFollowingTab = selectedTab == "following"
                        )
                    }
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                // Top padding already applied to PullToRefreshBox
                                bottom = innerPadding.calculateBottomPadding() + DesperseSizes.bottomNavHeight + DesperseSpacing.lg
                            )
                        ) {
                        items(
                            items = uiState.posts,
                            key = { it.id },
                            contentType = { "post" }
                        ) { post ->
                            // Use remember to stabilize lambdas and prevent recomposition
                            val onClickStable = remember(post.id) { { onPostClick(post.id) } }
                            val onUserClickStable = remember(post.user.slug) { { onUserClick(post.user.slug) } }
                            val onLikeClickStable = remember(post.id) { { viewModel.likePost(post.id) } }
                            // Use correct action based on post type
                            val onCollectClickStable = remember(post.id, post.type) {
                                {
                                    when (post.type) {
                                        "edition" -> viewModel.purchasePost(post.id, activity)
                                        "collectible" -> viewModel.collectPost(post.id)
                                        else -> {} // Regular posts have no collect action
                                    }
                                }
                            }
                            val onReportStable = remember(post.id) {
                                {
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
                            }
                            val onBlockStable = remember(post.user.id, post.user.slug) {
                                {
                                    blockUserId = post.user.id
                                    blockUserDisplayName = post.user.slug
                                    showBlockConfirmation = true
                                }
                            }
                            val onEditStable = remember(post.id) { { onEditPost(post.id) } }
                            val onDeleteStable = remember(post.id) {
                                {
                                    deletePostId = post.id
                                    showDeleteConfirmation = true
                                }
                            }
                            val isOwnPost = uiState.currentUserId != null && post.user.id == uiState.currentUserId
                            // Read per-item via derivedStateOf so only this item recomposes on change
                            val collectState by remember(post.id) {
                                derivedStateOf { collectStates[post.id] ?: CollectState.Idle }
                            }
                            val purchaseState by remember(post.id) {
                                derivedStateOf { purchaseStates[post.id] ?: PurchaseState.Idle }
                            }

                            val onCommentClickStable = remember(post.id, post.commentCount) {
                                {
                                    commentSheetViewModel.openForPost(post.id, post.commentCount)
                                    showCommentSheet = true
                                }
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
                                onBlock = onBlockStable,
                                onEditPost = onEditStable,
                                onDeletePost = onDeleteStable,
                                isOwnPost = isOwnPost,
                                collectState = collectState,
                                purchaseState = purchaseState
                            )
                        }

                            // Loading more indicator
                            if (uiState.isLoadingMore) {
                                item(key = "loading_more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = DesperseSpacing.lg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.height(24.dp).width(24.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                    }

                        // New posts toast - appears when scrolled down and new posts exist
                        NewPostsToast(
                            visible = showNewPostsToast,
                            creators = currentTabCreators,
                            onRefresh = {
                                scope.launch {
                                    // Scroll to top first
                                    listState.animateScrollToItem(0)
                                    // Then refresh
                                    viewModel.refresh()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Feed top bar with centered logo and tabs below.
 * Uses CenterAlignedTopAppBar for proper centering.
 * The whole bar hides on scroll with enterAlwaysScrollBehavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedTopBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onWalletClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    hasUnreadNotifications: Boolean = false,
    onTitleClick: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior,
    forYouNewCount: Int = 0,
    followingNewCount: Int = 0
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = {
                // Centered logo - tap to scroll to top
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onTitleClick
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.desperse_logo),
                        contentDescription = "Desperse",
                        modifier = Modifier.height(24.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Desperse",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            navigationIcon = {
                DesperseFaIconButton(
                    icon = FaIcons.Wallet,
                    onClick = onWalletClick,
                    contentDescription = "Wallet",
                    variant = ButtonVariant.Ghost,
                    style = FaIconStyle.Regular
                )
            },
            actions = {
                androidx.compose.ui.layout.Layout(
                    content = {
                        DesperseFaIconButton(
                            icon = FaIcons.Bell,
                            onClick = onNotificationsClick,
                            contentDescription = "Notifications",
                            variant = ButtonVariant.Ghost,
                            style = FaIconStyle.Regular
                        )
                        if (hasUnreadNotifications) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                ) { measurables, constraints ->
                    val buttonPlaceable = measurables[0].measure(constraints)
                    val badgePlaceable = measurables.getOrNull(1)?.measure(constraints)
                    layout(buttonPlaceable.width, buttonPlaceable.height) {
                        buttonPlaceable.placeRelative(0, 0)
                        if (badgePlaceable != null) {
                            badgePlaceable.placeRelative(
                                buttonPlaceable.width - badgePlaceable.width - 6,
                                6
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background
            ),
            scrollBehavior = scrollBehavior
        )

        // Tabs below the top bar
        FeedTabs(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            forYouNewCount = forYouNewCount,
            followingNewCount = followingNewCount
        )
    }
}

/**
 * Feed tabs (For You / Following)
 * Simple text tabs with underline indicator matching web design
 */
@Composable
private fun FeedTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    forYouNewCount: Int = 0,
    followingNewCount: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DesperseSpacing.xs)
    ) {
        FeedTab(
            text = "For You",
            isSelected = selectedTab == "for-you",
            onClick = { onTabSelected("for-you") },
            badgeCount = if (selectedTab != "for-you") forYouNewCount else 0,
            modifier = Modifier.weight(1f)
        )
        FeedTab(
            text = "Following",
            isSelected = selectedTab == "following",
            onClick = { onTabSelected("following") },
            badgeCount = if (selectedTab != "following") followingNewCount else 0,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual feed tab with underline indicator
 */
@Composable
private fun FeedTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "tabTextColor"
    )

    val underlineColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.background
        },
        label = "tabUnderlineColor"
    )

    Column(
        modifier = modifier
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = DesperseSpacing.sm)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            // Show badge for new posts on inactive tab
            if (badgeCount > 0) {
                Box(modifier = Modifier.padding(start = 6.dp)) {
                    NotificationBadge(count = badgeCount)
                }
            }
        }

        // Underline indicator with animated color
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(underlineColor)
        )
    }
}

/**
 * Empty state when no posts
 */
@Composable
private fun FeedEmptyState(
    isFollowingTab: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
        ) {
            Text(
                text = if (isFollowingTab) {
                    "Follow creators to see their posts"
                } else {
                    "No posts yet"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isFollowingTab) {
                DesperseTextButton(
                    text = "Explore",
                    onClick = { /* Navigate to explore */ },
                    variant = ButtonVariant.Secondary
                )
            }
        }
    }
}

/**
 * Error state
 */
@Composable
private fun FeedErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesperseSpacing.lg)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            DesperseTextButton(
                text = "Retry",
                onClick = onRetry,
                variant = ButtonVariant.Default
            )
        }
    }
}
