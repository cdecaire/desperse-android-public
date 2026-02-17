package app.desperse.ui.screens.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.app.Activity
import androidx.compose.ui.res.painterResource
import app.desperse.data.model.CollectState
import app.desperse.data.model.Post
import app.desperse.ui.components.ButtonVariant
import app.desperse.ui.components.DesperseFaIconButton
import app.desperse.ui.components.GeometricAvatar
import app.desperse.ui.components.GeometricBanner
import app.desperse.ui.components.DesperseTextButton
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.TipSheet
import app.desperse.ui.components.InstalledWallet
import app.desperse.ui.components.WalletPickerSheet
import app.desperse.R
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import app.desperse.ui.components.media.ImageContext
import app.desperse.ui.components.media.ImageOptimization
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    slug: String?,
    onPostClick: (String) -> Unit,
    onWalletClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onFollowersClick: ((String) -> Unit)? = null,
    onFollowingClick: ((String) -> Unit)? = null,
    onCollectorsClick: ((String) -> Unit)? = null,
    onActivityClick: (() -> Unit)? = null,
    onEditProfileClick: (() -> Unit)? = null,
    onMessageClick: ((ProfileViewModel.ConversationInfo) -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val gridState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isDmChecking by remember { mutableStateOf(false) }

    // Coroutine scope for showing snackbar messages
    val coroutineScope = rememberCoroutineScope()

    // Load more items when scrolling near the end
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (lastVisibleItem >= totalItems - 6 && !uiState.isLoadingMore && uiState.currentHasMore) {
                    viewModel.loadMore()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.user?.displayName ?: uiState.user?.slug ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { coroutineScope.launch { gridState.animateScrollToItem(0) } }
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            FaIcon(FaIcons.ArrowLeft, size = 20.dp)
                        }
                    }
                },
                actions = {
                    if (uiState.isOwnProfile && onWalletClick != null) {
                        DesperseFaIconButton(
                            icon = FaIcons.Wallet,
                            onClick = onWalletClick,
                            contentDescription = "Wallet",
                            variant = ButtonVariant.Ghost,
                            style = FaIconStyle.Regular
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            when {
                uiState.isLoading -> {
                    val shimmerBrush = app.desperse.ui.components.rememberShimmerBrush()
                    app.desperse.ui.components.ProfileSkeleton(brush = shimmerBrush)
                }

                uiState.error != null -> {
                    ProfileErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.refresh() }
                    )
                }

                uiState.user != null -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = padding.calculateBottomPadding() + DesperseSizes.bottomNavHeight + DesperseSpacing.lg
                        ),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Header banner and profile info as full-width items
                        item(span = { GridItemSpan(3) }) {
                            val profileSlug = uiState.user!!.slug
                            ProfileHeader(
                                user = uiState.user!!,
                                stats = uiState.stats,
                                followersCount = uiState.followersCount,
                                followingCount = uiState.followingCount,
                                collectorsCount = uiState.collectorsCount,
                                isFollowing = uiState.isFollowing,
                                isOwnProfile = uiState.isOwnProfile,
                                isFollowLoading = uiState.isFollowLoading,
                                isDmChecking = isDmChecking,
                                onFollowClick = { viewModel.toggleFollow() },
                                onFollowersClick = { onFollowersClick?.invoke(profileSlug) },
                                onFollowingClick = { onFollowingClick?.invoke(profileSlug) },
                                onCollectorsClick = { onCollectorsClick?.invoke(profileSlug) },
                                onActivityClick = if (uiState.isOwnProfile) onActivityClick else null,
                                onEditProfileClick = if (uiState.isOwnProfile) onEditProfileClick else null,
                                onMessageClick = if (!uiState.isOwnProfile && onMessageClick != null) {
                                    {
                                        if (!isDmChecking) {
                                            isDmChecking = true
                                            viewModel.startConversation(
                                                onSuccess = { info ->
                                                    isDmChecking = false
                                                    onMessageClick(info)
                                                },
                                                onError = { errorMsg ->
                                                    isDmChecking = false
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(errorMsg)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } else null,
                                onTipClick = if (!uiState.isOwnProfile) {
                                    { viewModel.showTipSheet() }
                                } else null
                            )
                        }

                        // Tabs
                        item(span = { GridItemSpan(3) }) {
                            ProfileTabs(
                                selectedTab = uiState.selectedTab,
                                postCount = uiState.stats?.posts ?: 0,
                                collectedCount = uiState.stats?.collected ?: 0,
                                forSaleCount = uiState.stats?.forSale ?: 0,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }

                        // Content grid based on selected tab
                        if (uiState.currentItems.isEmpty() && !uiState.isLoading && !uiState.isLoadingMore) {
                            item(span = { GridItemSpan(3) }) {
                                ProfileEmptyState(
                                    isOwnProfile = uiState.isOwnProfile,
                                    tab = uiState.selectedTab
                                )
                            }
                        } else {
                            items(
                                items = uiState.currentItems,
                                key = { it.id }
                            ) { post ->
                                ProfileGridItem(
                                    post = post,
                                    onClick = { onPostClick(post.id) }
                                )
                            }

                            // Loading more indicator
                            if (uiState.isLoadingMore) {
                                item(span = { GridItemSpan(3) }) {
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

    // Tip Sheet
    if (uiState.showTipSheet && uiState.user != null) {
        val activity = LocalContext.current as Activity
        TipSheet(
            creatorDisplayName = uiState.user!!.displayName,
            creatorAvatarUrl = uiState.user!!.avatarUrl,
            tipState = uiState.tipState,
            skrBalance = uiState.skrBalance,
            onSendTip = { amount -> viewModel.sendTip(amount, "profile", activity) },
            onDismiss = { viewModel.dismissTipSheet() }
        )
    }

    // Wallet Picker Sheet (shown when external wallet package is unknown)
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
}

@Composable
private fun ProfileHeader(
    user: app.desperse.data.dto.response.ProfileUser,
    stats: app.desperse.data.dto.response.ProfileStats?,
    followersCount: Int,
    followingCount: Int,
    collectorsCount: Int,
    isFollowing: Boolean,
    isOwnProfile: Boolean,
    isFollowLoading: Boolean,
    isDmChecking: Boolean = false,
    onFollowClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onCollectorsClick: () -> Unit,
    onActivityClick: (() -> Unit)?,
    onEditProfileClick: (() -> Unit)?,
    onMessageClick: (() -> Unit)? = null,
    onTipClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            if (user.headerBgUrl != null) {
                val optimizedHeaderUrl = remember(user.headerBgUrl) {
                    ImageOptimization.getOptimizedUrlForContext(user.headerBgUrl, ImageContext.PROFILE_HEADER)
                }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(optimizedHeaderUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Medium
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    GeometricBanner(
                        input = user.slug,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

        }

        // Profile content (overlapping the banner)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-40).dp)
                .padding(horizontal = DesperseSpacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                ) {
                    if (user.avatarUrl != null) {
                        val optimizedAvatarUrl = remember(user.avatarUrl) {
                            ImageOptimization.getOptimizedUrlForContext(user.avatarUrl, ImageContext.AVATAR)
                        }
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(optimizedAvatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = user.displayName ?: user.slug,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            GeometricAvatar(
                                input = user.slug,
                                size = 80.dp
                            )
                        }
                    }
                }

                // Action icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOwnProfile) {
                        // Own profile: Activity + Edit Profile
                        if (onActivityClick != null) {
                            IconButton(
                                onClick = onActivityClick,
                                modifier = Modifier.size(40.dp)
                            ) {
                                FaIcon(
                                    icon = FaIcons.Clock,
                                    size = 20.dp,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    style = FaIconStyle.Regular
                                )
                            }
                        }
                        // Edit profile icon
                        if (onEditProfileClick != null) {
                            IconButton(
                                onClick = onEditProfileClick,
                                modifier = Modifier.size(40.dp)
                            ) {
                                FaIcon(
                                    icon = FaIcons.UserPen,
                                    size = 20.dp,
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    style = FaIconStyle.Regular
                                )
                            }
                        }
                    } else {
                        // Other profile: Tip + Message + Follow
                        if (onTipClick != null) {
                            IconButton(
                                onClick = onTipClick,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_seeker_s),
                                    contentDescription = "Tip",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        if (onMessageClick != null) {
                            IconButton(
                                onClick = onMessageClick,
                                enabled = !isDmChecking,
                                modifier = Modifier.size(40.dp)
                            ) {
                                if (isDmChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                } else {
                                    FaIcon(
                                        icon = FaIcons.PaperPlane,
                                        size = 20.dp,
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        style = FaIconStyle.Regular
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = onFollowClick,
                            enabled = !isFollowLoading,
                            modifier = Modifier.size(40.dp)
                        ) {
                            FaIcon(
                                icon = if (isFollowing) FaIcons.UserCheck else FaIcons.UserPlus,
                                size = 20.dp,
                                tint = MaterialTheme.colorScheme.onBackground,
                                style = FaIconStyle.Regular
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesperseSpacing.md))

            // Name and handle
            Text(
                text = user.displayName ?: user.slug,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "@${user.slug}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Bio
            if (!user.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(DesperseSpacing.sm))
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Link
            if (!user.link.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(DesperseSpacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FaIcon(
                        icon = FaIcons.Link,
                        size = 14.dp,
                        tint = MaterialTheme.colorScheme.primary,
                        style = FaIconStyle.Solid
                    )
                    Spacer(modifier = Modifier.width(DesperseSpacing.xs))
                    Text(
                        text = user.link.removePrefix("https://").removePrefix("http://").removeSuffix("/"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesperseSpacing.md))

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xl)
            ) {
                ProfileStatItem(
                    count = followersCount,
                    label = if (followersCount == 1) "follower" else "followers",
                    onClick = onFollowersClick
                )
                ProfileStatItem(
                    count = followingCount,
                    label = "following",
                    onClick = onFollowingClick
                )
                ProfileStatItem(
                    count = collectorsCount,
                    label = if (collectorsCount == 1) "collector" else "collectors",
                    onClick = onCollectorsClick
                )
            }
        }
    }
}

@Composable
private fun ProfileStatItem(
    count: Int,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileTabs(
    selectedTab: ProfileTab,
    postCount: Int,
    collectedCount: Int,
    forSaleCount: Int,
    onTabSelected: (ProfileTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DesperseSpacing.sm)
    ) {
        ProfileTab(
            count = postCount,
            label = "Posts",
            isSelected = selectedTab == ProfileTab.Posts,
            onClick = { onTabSelected(ProfileTab.Posts) },
            modifier = Modifier.weight(1f)
        )
        ProfileTab(
            count = collectedCount,
            label = "Collected",
            isSelected = selectedTab == ProfileTab.Collected,
            onClick = { onTabSelected(ProfileTab.Collected) },
            modifier = Modifier.weight(1f)
        )
        ProfileTab(
            count = forSaleCount,
            label = "For Sale",
            isSelected = selectedTab == ProfileTab.ForSale,
            onClick = { onTabSelected(ProfileTab.ForSale) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProfileTab(
    count: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
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
            Color.Transparent
        },
        label = "tabUnderlineColor"
    )

    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.padding(vertical = DesperseSpacing.sm),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }

        Box(
            modifier = Modifier
                .width(60.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(underlineColor)
        )
    }
}

@Composable
private fun ProfileGridItem(
    post: Post,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // Detect if the media is a video
    val isVideo = post.mediaUrl?.let { url ->
        val extension = url.substringAfterLast('.').substringBefore('?').lowercase()
        extension in listOf("mp4", "webm", "mov")
    } ?: false

    // For videos: use coverUrl only (don't fallback to mediaUrl since AsyncImage can't render videos)
    // For images: use mediaUrl
    val imageUrl = if (isVideo) post.coverUrl else (post.coverUrl ?: post.mediaUrl)
    val showVideoPlaceholder = isVideo && imageUrl == null

    val optimizedImageUrl = remember(imageUrl) {
        imageUrl?.let { ImageOptimization.getOptimizedUrlForContext(it, ImageContext.PROFILE_GRID) }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        if (optimizedImageUrl != null) {
            AsyncImage(
                model = remember(optimizedImageUrl) {
                    ImageRequest.Builder(context)
                        .data(optimizedImageUrl)
                        .crossfade(true)
                        .build()
                },
                contentDescription = post.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low
            )
        }

        // Video placeholder when no cover image is available
        if (showVideoPlaceholder) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                FaIcon(
                    icon = FaIcons.Play,
                    size = 32.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    style = FaIconStyle.Solid
                )
            }
        }

        // Video indicator badge (top right) - shows for videos WITH covers
        if (isVideo && imageUrl != null) {
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
        // Collectible/Edition badge (top right) - only for non-videos or when video has no badge
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

@Composable
private fun ProfileEmptyState(
    isOwnProfile: Boolean,
    tab: ProfileTab
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(DesperseSpacing.xxl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesperseSpacing.md)
        ) {
            val (icon, title, description) = when (tab) {
                ProfileTab.Posts -> Triple(
                    FaIcons.Images,
                    "No posts yet",
                    if (isOwnProfile) "Share your first creation!" else "This user hasn't created any posts yet."
                )
                ProfileTab.Collected -> Triple(
                    FaIcons.Gem,
                    "No collections yet",
                    if (isOwnProfile) "You haven't collected any NFTs yet." else "This user hasn't collected any NFTs yet."
                )
                ProfileTab.ForSale -> Triple(
                    FaIcons.Tag,
                    "Nothing for sale",
                    if (isOwnProfile) "You don't have any editions for sale." else "This user doesn't have any editions for sale."
                )
            }

            FaIcon(
                icon = icon,
                size = 48.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                style = FaIconStyle.Regular
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ProfileErrorState(
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
            FaIcon(
                icon = FaIcons.TriangleExclamation,
                size = 48.dp,
                tint = MaterialTheme.colorScheme.error,
                style = FaIconStyle.Solid
            )
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
