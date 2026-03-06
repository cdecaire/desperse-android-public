package app.desperse.ui.screens.post

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import app.desperse.ui.components.AvatarSize
import app.desperse.ui.components.DesperseAvatar
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.util.formatCount
import app.desperse.ui.util.formatRelativeTime
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.desperse.data.model.CollectState
import app.desperse.data.model.Post
import app.desperse.data.model.PurchaseState
import app.desperse.ui.components.ButtonVariant
import app.desperse.ui.components.CommentSheet
import app.desperse.ui.components.DesperseFaIconButton
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.MentionText
import app.desperse.ui.components.InstalledWallet
import app.desperse.ui.components.PostCardMenuSheet
import app.desperse.ui.components.ReportContentPreview
import app.desperse.ui.components.ReportSheet
import app.desperse.ui.components.WalletPickerSheet
import app.desperse.ui.components.DesperseTextButton
import app.desperse.ui.components.EmptyState
import app.desperse.ui.components.GeometricAvatar
import app.desperse.ui.components.LoadingMoreIndicator
import app.desperse.ui.components.LoadingState
import app.desperse.ui.components.media.ImageContext
import app.desperse.ui.components.media.ImageOptimization
import app.desperse.ui.components.media.MediaType
import app.desperse.ui.components.media.PostMedia
import app.desperse.ui.components.media.detectMediaType
import app.desperse.ui.components.media.detectMediaTypeFromMime
import app.desperse.data.dto.response.FollowUser
import app.desperse.ui.screens.feed.CommentSheetViewModel
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.DesperseTones
import app.desperse.ui.theme.toneCollectible
import app.desperse.ui.theme.toneEdition
import app.desperse.ui.theme.toneLike
import app.desperse.ui.util.MintWindowPhase
import app.desperse.ui.util.MintWindowUtils
import coil.compose.AsyncImage
import android.app.Activity
import coil.request.ImageRequest
import java.time.Instant
import java.time.temporal.ChronoUnit
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.desperse.core.preferences.ExplorerOption
import app.desperse.core.util.openInAppBrowser
import app.desperse.ui.components.PostCardMenuEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onEditPost: (String) -> Unit = {},
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as Activity
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showDeletePostConfirmation by remember { mutableStateOf(false) }

    // Download manager for gated assets
    val downloadManager = remember {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            PostCardMenuEntryPoint::class.java
        )
        entryPoint.gatedDownloadManager()
    }

    // Comment sheet state
    var showCommentSheet by remember { mutableStateOf(false) }
    val commentSheetViewModel: CommentSheetViewModel = hiltViewModel()

    // Report state
    var showReportSheet by remember { mutableStateOf(false) }
    var reportContentType by remember { mutableStateOf("post") }
    var reportContentId by remember { mutableStateOf("") }
    var reportContentPreview by remember { mutableStateOf<ReportContentPreview?>(null) }

    // Lifecycle observer for periodic refresh of post counts
    val lifecycleOwner = LocalLifecycleOwner.current
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

    val post = uiState.post
    val isNft = post != null && (post.type == "collectible" || post.type == "edition")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        FaIcon(FaIcons.ArrowLeft, size = 20.dp)
                    }
                },
                actions = {
                    if (post != null) {
                        DesperseFaIconButton(
                            icon = FaIcons.EllipsisVertical,
                            onClick = { showMenu = true },
                            contentDescription = "More options",
                            variant = ButtonVariant.Ghost
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (isNft) {
                StickyFooterCta(
                    post = post,
                    collectState = uiState.collectState,
                    purchaseState = uiState.purchaseState,
                    onCollectClick = { viewModel.collect() },
                    onPurchaseClick = { viewModel.purchase(activity) },
                    onDownloadClick = {
                        // Priority: assetId > downloadableAssets[0].id (matches web)
                        val downloadAssetId = post.assetId
                            ?: post.downloadableAssets?.firstOrNull()?.id
                            ?: return@StickyFooterCta
                        coroutineScope.launch {
                            downloadManager.downloadGatedAsset(context, downloadAssetId)
                        }
                    }
                )
            }
        }
    ) { scaffoldPadding ->
        when {
            uiState.isLoadingPost && post == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null && post == null -> {
                ErrorState(
                    message = uiState.error ?: "Failed to load post",
                    onRetry = { viewModel.loadPost() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                )
            }
            post != null -> {
                PostDetailContent(
                    post = post,
                    uiState = uiState,
                    purchaseState = uiState.purchaseState,
                    onUserClick = onUserClick,
                    onLikeClick = { viewModel.toggleLike() },
                    onCommentClick = {
                        commentSheetViewModel.openForPost(postId, post.commentCount)
                        showCommentSheet = true
                    },
                    onLoadCollectors = { viewModel.loadCollectors() },
                    onLoadMoreCollectors = { viewModel.loadMoreCollectors() },
                    onToggleFollowCollector = { viewModel.toggleFollowCollector(it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                )
            }
        }

        // Post menu sheet
        uiState.post?.let { menuPost ->
            val isOwnPost = uiState.currentUserId != null && menuPost.user.id == uiState.currentUserId
            PostCardMenuSheet(
                isOpen = showMenu,
                post = menuPost,
                onDismiss = { showMenu = false },
                onGoToPost = { /* Already on detail page */ },
                onReport = {
                    reportContentType = "post"
                    reportContentId = menuPost.id
                    reportContentPreview = ReportContentPreview(
                        userName = menuPost.user.displayName ?: menuPost.user.slug,
                        userAvatarUrl = menuPost.user.avatarUrl,
                        contentText = menuPost.caption,
                        mediaUrl = menuPost.coverUrl ?: menuPost.mediaUrl
                    )
                    showReportSheet = true
                },
                onEditPost = { onEditPost(menuPost.id) },
                onDeletePost = { showDeletePostConfirmation = true },
                hideGoToPost = true,
                hasDownloadAccess = menuPost.isCollected || isOwnPost,
                isOwnPost = isOwnPost
            )
        }

        // Delete post confirmation dialog
        if (showDeletePostConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeletePostConfirmation = false },
                title = { Text("Delete Post") },
                text = { Text("Are you sure you want to delete this post? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeletePostConfirmation = false
                            viewModel.deletePost(onDeleted = onBack)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeletePostConfirmation = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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
                reportContentId = comment.id
                reportContentPreview = ReportContentPreview(
                    userName = comment.user.displayName ?: comment.user.slug,
                    userAvatarUrl = comment.user.avatarUrl,
                    contentText = comment.content
                )
                showReportSheet = true
            },
            viewModel = commentSheetViewModel
        )

        // Report sheet
        ReportSheet(
            open = showReportSheet,
            onDismiss = { showReportSheet = false },
            contentType = reportContentType,
            contentPreview = reportContentPreview ?: ReportContentPreview(userName = ""),
            onSubmit = { reasons, details ->
                viewModel.createReport(reportContentType, reportContentId, reasons, details)
            }
        )

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
}

/**
 * Sticky footer CTA for NFT posts (collectible/edition).
 * Uses DesperseButton(variant=Default, size=Cta) full-width.
 */
@Composable
private fun StickyFooterCta(
    post: Post,
    collectState: CollectState,
    purchaseState: PurchaseState,
    onCollectClick: () -> Unit,
    onPurchaseClick: () -> Unit,
    onDownloadClick: () -> Unit = {}
) {
    val isEdition = post.type == "edition"

    val mintPhase = remember(post.mintWindowStart, post.mintWindowEnd) {
        MintWindowUtils.getMintWindowPhase(post.mintWindowStart, post.mintWindowEnd)
    }

    val isCollected = post.isCollected ||
            collectState is CollectState.Success ||
            purchaseState is PurchaseState.Success
    val isFailed = (collectState is CollectState.Failed && !isCollected) ||
            (purchaseState is PurchaseState.Failed && !isCollected)
    val isLoading = collectState is CollectState.Preparing ||
            collectState is CollectState.Confirming ||
            purchaseState is PurchaseState.Preparing ||
            purchaseState is PurchaseState.Signing ||
            purchaseState is PurchaseState.Broadcasting ||
            purchaseState is PurchaseState.Submitting ||
            purchaseState is PurchaseState.Confirming

    val isSoldOut = isEdition && post.maxSupply != null && post.maxSupply > 0 &&
            (post.currentSupply ?: 0) >= post.maxSupply
    val isMintScheduled = mintPhase is MintWindowPhase.Scheduled
    val isMintEnded = mintPhase is MintWindowPhase.Ended
    // Match web logic: check downloadableAssets OR media type detection (document/3D)
    val mediaType = remember(post.mediaMimeType, post.mediaUrl) {
        if (post.mediaMimeType != null) detectMediaTypeFromMime(post.mediaMimeType)
        else detectMediaType(post.mediaUrl)
    }
    val hasDownload = !post.downloadableAssets.isNullOrEmpty() ||
            mediaType == MediaType.DOCUMENT ||
            mediaType == MediaType.MODEL_3D

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        if (isMintEnded && post.mintWindowEnd != null && !(isCollected && hasDownload)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesperseSpacing.md, vertical = DesperseSpacing.sm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mint ended ${MintWindowUtils.formatDateTime(post.mintWindowEnd)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val isDownloadMode = isCollected && hasDownload
            val buttonText = when {
                isDownloadMode -> "Download"
                isCollected -> "Collected"
                isFailed -> "Try Again"
                isSoldOut -> "Sold Out"
                isMintScheduled -> "Not Yet Available"
                isEdition && post.price != null && post.price > 0 -> {
                    "Collect \u00B7 ${formatPriceText(post.price, post.currency)}"
                }
                else -> "Collect"
            }
            val isEnabled = isDownloadMode || (!isCollected && !isSoldOut && !isLoading && !isMintScheduled)
            val containerColor = MaterialTheme.colorScheme.onSurface
            val contentColor = MaterialTheme.colorScheme.surface

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesperseSpacing.md)
                    .padding(top = DesperseSpacing.lg, bottom = DesperseSpacing.sm)
            ) {
                Button(
                    onClick = {
                        when {
                            isDownloadMode -> onDownloadClick()
                            isEdition -> onPurchaseClick()
                            else -> onCollectClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DesperseSizes.buttonCta),
                    enabled = isEnabled,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = containerColor,
                        contentColor = contentColor,
                        disabledContainerColor = containerColor.copy(alpha = 0.5f),
                        disabledContentColor = contentColor.copy(alpha = 0.5f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = contentColor
                        )
                    } else {
                        if (isDownloadMode) {
                            FaIcon(
                                icon = FaIcons.Download,
                                size = 14.dp,
                                tint = contentColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else if (isCollected) {
                            FaIcon(
                                icon = FaIcons.Check,
                                size = 14.dp,
                                tint = contentColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = buttonText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostDetailContent(
    post: Post,
    uiState: PostDetailUiState,
    purchaseState: PurchaseState,
    onUserClick: (String) -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onLoadCollectors: () -> Unit,
    onLoadMoreCollectors: () -> Unit,
    onToggleFollowCollector: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isNft = post.type == "collectible" || post.type == "edition"
    var selectedTab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    // Load collectors when switching to that tab
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && isNft) {
            onLoadCollectors()
        }
    }

    // Infinite scroll for collectors tab
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                if (selectedTab == 1) {
                    val totalItems = layoutInfo.totalItemsCount
                    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    if (lastVisibleItem >= totalItems - 6 &&
                        !uiState.isLoadingMoreCollectors &&
                        uiState.hasMoreCollectors
                    ) {
                        onLoadMoreCollectors()
                    }
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize()
    ) {
        // User Header
        item(key = "header") {
            PostDetailHeader(
                post = post,
                onUserClick = { onUserClick(post.user.slug) }
            )
        }

        // Media
        item(key = "media") {
            Box(modifier = Modifier.fillMaxWidth()) {
                PostMedia(
                    post = post,
                    useFixedAspectRatio = false,
                    modifier = Modifier.fillMaxWidth()
                )

                if (post.type == "edition") {
                    val priceText = formatPriceText(post.price, post.currency)
                    MediaPill(
                        text = priceText,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 24.dp, top = 12.dp)
                    )
                }
            }
        }

        // Action Bar
        item(key = "actions") {
            PostDetailActions(
                post = post,
                purchaseState = purchaseState,
                onLikeClick = onLikeClick,
                onCommentClick = onCommentClick
            )
        }

        // NFT Name title
        if (!post.nftName.isNullOrBlank()) {
            item(key = "nft_name") {
                Text(
                    text = post.nftName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(
                        horizontal = DesperseSpacing.md
                    ).padding(top = DesperseSpacing.sm)
                )
            }
        }

        // Caption
        if (!post.caption.isNullOrBlank()) {
            item(key = "caption") {
                PostDetailCaption(
                    caption = post.caption,
                    onMentionClick = onUserClick
                )
            }
        }

        // Segment control + tab content
        item(key = "divider") {
            Spacer(modifier = Modifier.height(DesperseSpacing.lg))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item(key = "tabs") {
            DetailTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                showCollectors = isNft
            )
        }

        // Full-width divider above tab content
        item(key = "tab_divider") {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        // Tab content
        when {
            selectedTab == 0 || !isNft -> {
                item(key = "details") {
                    PostDetailsSection(post = post)
                }
            }
            selectedTab == 1 && isNft -> {
                when {
                    uiState.isLoadingCollectors -> {
                        item(key = "collectors_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    uiState.collectorsError != null -> {
                        item(key = "collectors_error") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DesperseSpacing.xl),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.collectorsError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    uiState.collectors.isEmpty() -> {
                        item(key = "collectors_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DesperseSpacing.xl),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    FaIcon(
                                        icon = FaIcons.Users,
                                        size = 32.dp,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(DesperseSpacing.sm))
                                    Text(
                                        text = "No collectors yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        items(
                            items = uiState.collectors,
                            key = { it.id },
                            contentType = { "collector" }
                        ) { user ->
                            val onUserClickStable = remember(user.slug) { { onUserClick(user.slug) } }
                            val onFollowClickStable = remember(user.id) { { onToggleFollowCollector(user.id) } }
                            CollectorItem(
                                user = user,
                                isCurrentUser = user.id == uiState.currentUserId,
                                onUserClick = onUserClickStable,
                                onFollowClick = onFollowClickStable,
                                isFollowLoading = uiState.collectorsFollowLoadingIds.contains(user.id)
                            )
                        }

                        if (uiState.isLoadingMoreCollectors) {
                            item(key = "collectors_loading_more") {
                                LoadingMoreIndicator()
                            }
                        }
                    }
                }
            }
        }

        // Bottom spacing
        item(key = "bottom_spacing") {
            Spacer(modifier = Modifier.height(DesperseSpacing.xl))
        }
    }
}

@Composable
private fun PostDetailHeader(
    post: Post,
    onUserClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = DesperseSpacing.md, vertical = DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        DesperseAvatar(
            imageUrl = post.user.avatarUrl,
            contentDescription = "${post.user.displayName ?: post.user.slug}'s avatar",
            identityInput = post.user.walletAddress ?: post.user.slug,
            size = AvatarSize.Medium
        )

        Spacer(modifier = Modifier.width(DesperseSpacing.sm))

        // User info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = post.user.displayName ?: post.user.slug,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "@${post.user.slug}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "\u00B7",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatRelativeTime(post.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Action bar with like + comment on left, passive collect/edition count on right.
 * No clickable collect/buy buttons — CTA is in the sticky footer.
 */
@Composable
private fun PostDetailActions(
    post: Post,
    purchaseState: PurchaseState,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.md, vertical = DesperseSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Like + Comment count
        Row(
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like button
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onLikeClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FaIcon(
                    icon = FaIcons.Heart,
                    size = 18.dp,
                    style = if (post.isLiked) FaIconStyle.Solid else FaIconStyle.Regular,
                    tint = if (post.isLiked) toneLike() else MaterialTheme.colorScheme.onSurface
                )
                if (post.likeCount > 0) {
                    Text(
                        text = post.likeCount.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Comment count
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onCommentClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FaIcon(
                    icon = FaIcons.Comment,
                    size = 18.dp,
                    style = FaIconStyle.Regular
                )
                if (post.commentCount > 0) {
                    Text(
                        text = post.commentCount.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Right side: Passive collect/edition count (display-only)
        when (post.type) {
            "collectible" -> {
                val toneColor = if (post.isCollected) toneCollectible()
                    else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FaIcon(
                        icon = FaIcons.Gem,
                        size = 18.dp,
                        style = if (post.isCollected) FaIconStyle.Solid else FaIconStyle.Regular,
                        tint = toneColor
                    )
                    if (post.collectCount > 0) {
                        Text(
                            text = formatCount(post.collectCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = toneColor
                        )
                    }
                }
            }
            "edition" -> {
                val isOwned = post.isCollected || purchaseState is PurchaseState.Success
                val toneColor = if (isOwned) toneEdition()
                    else MaterialTheme.colorScheme.onSurfaceVariant
                val icon = if (post.maxSupply == 1) FaIcons.HexagonImage else FaIcons.Images
                val supplyText = when {
                    post.maxSupply == 1 -> "1/1"
                    post.maxSupply != null && post.maxSupply > 0 ->
                        "${post.currentSupply ?: 0}/${post.maxSupply}"
                    else -> formatCount(post.currentSupply ?: 0)
                }
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FaIcon(
                        icon = icon,
                        size = 18.dp,
                        style = if (isOwned) FaIconStyle.Solid else FaIconStyle.Regular,
                        tint = toneColor
                    )
                    Text(
                        text = supplyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = toneColor
                    )
                }
            }
        }
    }
}

@Composable
private fun PostDetailCaption(
    caption: String,
    onMentionClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.md, vertical = DesperseSpacing.xs)
    ) {
        MentionText(
            text = caption,
            onMentionClick = onMentionClick,
            style = MaterialTheme.typography.bodyMedium,
            textColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Details section showing post metadata.
 * Adapts fields shown based on post type (standard, collectible, edition).
 */
@Composable
private fun PostDetailsSection(post: Post) {
    val context = LocalContext.current
    val isNft = post.type == "collectible" || post.type == "edition"
    val hasMintWindow = post.mintWindowStart != null && post.mintWindowEnd != null

    // Explorer preference for Token ID link
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PostCardMenuEntryPoint::class.java
        )
    }
    val explorerOption by entryPoint.appPreferences().explorer.collectAsState(initial = ExplorerOption.ORB)

    // Type label
    val typeLabel = when (post.type) {
        "collectible" -> "Collectible"
        "edition" -> when {
            hasMintWindow -> "Timed Edition"
            post.maxSupply == null || post.maxSupply == 0 -> "Open Edition"
            post.maxSupply == 1 -> "1/1"
            else -> "Limited Edition"
        }
        else -> "Post"
    }

    // Categories
    val categoriesText = post.categories?.takeIf { it.isNotEmpty() }?.joinToString(", ")

    // Supply (NFT only)
    val supplyText = when (post.type) {
        "collectible" -> "${formatCount(post.collectCount)} collected"
        "edition" -> {
            val current = post.currentSupply ?: 0
            when {
                post.maxSupply == 1 -> "1 / 1"
                post.maxSupply != null && post.maxSupply > 0 ->
                    "${java.text.NumberFormat.getIntegerInstance().format(current)} / ${java.text.NumberFormat.getIntegerInstance().format(post.maxSupply)}"
                else ->
                    "${java.text.NumberFormat.getIntegerInstance().format(current)} / \u221E"
            }
        }
        else -> null
    }

    // Price (NFT only)
    val priceLabel = when {
        !isNft -> null
        post.type == "collectible" -> "Free"
        post.price != null && post.price > 0 -> formatPriceText(post.price, post.currency)
        else -> "Free"
    }

    // Media type
    val mediaTypeLabel = remember(post.mediaMimeType, post.mediaUrl, post.assets, post.downloadableAssets) {
        val mime = post.mediaMimeType
            ?: post.assets?.firstOrNull()?.mimeType
            ?: post.downloadableAssets?.firstOrNull()?.mimeType
        val mediaType = if (mime != null) {
            detectMediaTypeFromMime(mime)
        } else {
            detectMediaType(post.mediaUrl)
        }
        when (mediaType) {
            MediaType.IMAGE -> "Image"
            MediaType.VIDEO -> "Video"
            MediaType.AUDIO -> "Audio"
            MediaType.DOCUMENT -> "Document"
            MediaType.MODEL_3D -> "3D Model"
        }
    }

    // File size
    val fileSize = post.mediaFileSize
        ?: post.assets?.firstOrNull()?.fileSize
        ?: post.downloadableAssets?.firstOrNull()?.fileSize
    val fileSizeText = fileSize?.let { formatFileSize(it) }

    // Storage
    val storageText = when (post.storageType) {
        "arweave" -> when (post.arweaveStatus) {
            "uploaded" -> "Permanent (Arweave)"
            "funded", "uploading" -> "Uploading to Arweave"
            "failed" -> "Upload Failed"
            else -> "Arweave"
        }
        else -> "Centralized"
    }

    // Token standard (NFT only)
    val tokenStandard = when (post.type) {
        "collectible" -> "Compressed NFT"
        "edition" -> "Metaplex Core"
        else -> null
    }

    // Token ID (NFT only)
    val tokenId = when (post.type) {
        "collectible" -> post.collectibleAssetId
        "edition" -> post.masterMint
        else -> null
    }

    // Mint window (NFT only)
    val mintWindowText = if (hasMintWindow) {
        "${MintWindowUtils.formatDateTime(post.mintWindowStart!!)} \u2014 ${MintWindowUtils.formatDateTime(post.mintWindowEnd!!)}"
    } else null

    // Posted date (standard posts show full date since they don't have NFT metadata)
    val postedDateText = if (!isNft) {
        formatFullDate(post.createdAt)
    } else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.md)
            .padding(top = DesperseSpacing.md)
    ) {
        DetailRow("Type", typeLabel)
        categoriesText?.let { DetailRow("Categories", it) }
        supplyText?.let { DetailRow("Supply", it) }
        priceLabel?.let { DetailRow("Price", it) }
        DetailRow("Media", mediaTypeLabel)
        fileSizeText?.let { DetailRow("File Size", it) }
        DetailRow("Storage", storageText)
        postedDateText?.let { DetailRow("Posted", it) }
        tokenStandard?.let { DetailRow("Token Standard", it) }
        tokenId?.takeIf { it.isNotBlank() }?.let { id ->
            DetailRow(
                label = "Token ID",
                value = "${id.take(4)}...${id.takeLast(4)}",
                trailingIcon = FaIcons.ArrowUpRightFromSquare,
                onClick = {
                    val url = explorerOption.getExplorerUrl(id)
                    context.openInAppBrowser(url)
                }
            )
        }
        mintWindowText?.let { DetailRow("Mint Window", it) }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    trailingIcon: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (trailingIcon != null) {
                FaIcon(
                    icon = trailingIcon,
                    size = 12.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Segment control tabs matching the feed "For You / Following" style.
 */
@Composable
private fun DetailTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showCollectors: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = DesperseSpacing.xs,
                start = if (showCollectors) 0.dp else DesperseSpacing.md,
                end = if (showCollectors) 0.dp else DesperseSpacing.md
            )
    ) {
        DetailTab(
            text = "Details",
            isSelected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            modifier = if (showCollectors) Modifier.weight(1f) else Modifier
        )
        if (showCollectors) {
            DetailTab(
                text = "Collectors",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DetailTab(
    text: String,
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
            MaterialTheme.colorScheme.background
        },
        label = "tabUnderlineColor"
    )

    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(vertical = DesperseSpacing.sm)
        )

        Box(
            modifier = Modifier
                .width(48.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(underlineColor)
        )
    }
}

@Composable
private fun CollectorItem(
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

            DesperseTextButton(
                text = if (user.isFollowing) "Following" else "Follow",
                onClick = onFollowClick,
                variant = if (user.isFollowing) ButtonVariant.Secondary else ButtonVariant.Default,
                enabled = !isFollowLoading
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FaIcon(
            icon = FaIcons.CircleExclamation,
            size = 48.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(DesperseSpacing.md))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(DesperseSpacing.md))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

// Helper data class for post type info
/**
 * Price pill overlay on media - matches web app MediaPill component
 * Style: small rounded pill with dark background
 */
@Composable
private fun MediaPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}

/**
 * Format price for display
 */
private fun formatPriceText(price: Double?, currency: String?): String {
    return if (price != null && price > 0) {
        when (currency) {
            "SOL" -> "%.2f SOL".format(price / 1_000_000_000.0)
            "USDC" -> "$%.2f".format(price / 1_000_000.0)
            else -> "$price"
        }
    } else {
        "Free"
    }
}


/**
 * Format file size in bytes to human-readable string.
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}

/**
 * Format a timestamp to full date (e.g., "Mar 5, 2026")
 */
private fun formatFullDate(timestamp: String): String {
    return try {
        val instant = Instant.parse(timestamp)
        val date = java.time.LocalDate.ofInstant(instant, java.time.ZoneId.systemDefault())
        val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        "$month ${date.dayOfMonth}, ${date.year}"
    } catch (e: Exception) {
        ""
    }
}

