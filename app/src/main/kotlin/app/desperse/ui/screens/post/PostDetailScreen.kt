package app.desperse.ui.screens.post

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.desperse.data.dto.response.Comment
import app.desperse.data.model.CollectState
import app.desperse.data.model.Post
import app.desperse.data.model.PurchaseState
import app.desperse.ui.components.CollectButton
import app.desperse.ui.components.DesperseFaIconButton
import app.desperse.ui.components.ButtonVariant
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.components.MentionText
import app.desperse.ui.components.MentionTextField
import app.desperse.ui.components.InstalledWallet
import app.desperse.ui.components.PostCardMenuSheet
import app.desperse.ui.components.ReportContentPreview
import app.desperse.ui.components.ReportSheet
import app.desperse.ui.components.SwipeableCommentItem
import app.desperse.ui.components.WalletPickerSheet
import app.desperse.data.dto.response.MentionUser
import app.desperse.ui.components.media.PostMedia
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.DesperseTones
import coil.compose.AsyncImage
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import android.app.Activity
import java.time.Instant
import java.time.temporal.ChronoUnit
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

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
    var showMenu by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var previousCommentsSize by remember { mutableStateOf(0) }
    val focusManager = LocalFocusManager.current
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }
    var showDeletePostConfirmation by remember { mutableStateOf(false) }

    // Report state
    var showReportSheet by remember { mutableStateOf(false) }
    var reportContentType by remember { mutableStateOf("post") }
    var reportContentId by remember { mutableStateOf("") }
    var reportContentPreview by remember { mutableStateOf<ReportContentPreview?>(null) }

    // Clear comment text after successful submission (when comments list grows)
    LaunchedEffect(uiState.comments.size, uiState.isSubmittingComment) {
        if (!uiState.isSubmittingComment && uiState.comments.size > previousCommentsSize) {
            commentText = ""
        }
        previousCommentsSize = uiState.comments.size
    }

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
                    if (uiState.post != null) {
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { scaffoldPadding ->
        when {
            uiState.isLoadingPost && uiState.post == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null && uiState.post == null -> {
                ErrorState(
                    message = uiState.error ?: "Failed to load post",
                    onRetry = { viewModel.loadPost() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                )
            }
            uiState.post != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                ) {
                    PostDetailContent(
                        post = uiState.post!!,
                        comments = uiState.comments,
                        isLoadingComments = uiState.isLoadingComments,
                        collectState = uiState.collectState,
                        purchaseState = uiState.purchaseState,
                        currentUserId = uiState.currentUserId,
                        deletingCommentId = uiState.deletingCommentId,
                        onUserClick = onUserClick,
                        onLikeClick = { viewModel.toggleLike() },
                        onCollectClick = { viewModel.collect() },
                        onPurchaseClick = { viewModel.purchase(activity) },
                        onDeleteComment = { comment -> commentToDelete = comment },
                        onReportComment = { comment ->
                            reportContentType = "comment"
                            reportContentId = comment.id
                            reportContentPreview = ReportContentPreview(
                                userName = comment.user.displayName ?: comment.user.slug,
                                userAvatarUrl = comment.user.avatarUrl,
                                contentText = comment.content
                            )
                            showReportSheet = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                    CommentInputBar(
                        text = commentText,
                        onTextChange = { commentText = it },
                        onSubmit = {
                            if (commentText.trim().isNotEmpty()) {
                                viewModel.createComment(commentText)
                                commentText = ""
                                focusManager.clearFocus()
                            }
                        },
                        onSearch = { query -> viewModel.searchMentionUsers(query) },
                        isSubmitting = uiState.isSubmittingComment,
                        error = uiState.commentError,
                        onErrorDismiss = { viewModel.clearCommentError() },
                        avatarUrl = uiState.currentUserAvatarUrl,
                        modifier = Modifier
                            .imePadding()
                            .navigationBarsPadding()
                    )
                }
            }
        }

        // Post menu sheet
        uiState.post?.let { post ->
            val isOwnPost = uiState.currentUserId != null && post.user.id == uiState.currentUserId
            PostCardMenuSheet(
                isOpen = showMenu,
                post = post,
                onDismiss = { showMenu = false },
                onGoToPost = { /* Already on detail page */ },
                onReport = {
                    reportContentType = "post"
                    reportContentId = post.id
                    reportContentPreview = ReportContentPreview(
                        userName = post.user.displayName ?: post.user.slug,
                        userAvatarUrl = post.user.avatarUrl,
                        contentText = post.caption,
                        mediaUrl = post.coverUrl ?: post.mediaUrl
                    )
                    showReportSheet = true
                },
                onEditPost = { onEditPost(post.id) },
                onDeletePost = { showDeletePostConfirmation = true },
                hideGoToPost = true,
                hasDownloadAccess = post.isCollected || isOwnPost,
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

        // Delete comment confirmation dialog
        commentToDelete?.let { comment ->
            AlertDialog(
                onDismissRequest = { commentToDelete = null },
                title = { Text("Delete comment?") },
                text = { Text("This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteComment(comment.id)
                            commentToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { commentToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

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

@Composable
private fun PostDetailContent(
    post: Post,
    comments: List<Comment>,
    isLoadingComments: Boolean,
    collectState: CollectState,
    purchaseState: PurchaseState,
    currentUserId: String?,
    deletingCommentId: String?,
    onUserClick: (String) -> Unit,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onPurchaseClick: () -> Unit,
    onDeleteComment: (Comment) -> Unit,
    onReportComment: (Comment) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        // User Header
        item {
            PostDetailHeader(
                post = post,
                onUserClick = { onUserClick(post.user.slug) }
            )
        }

        // Media - use dynamic aspect ratio for detail view (no layout shift concerns)
        // Includes price pill overlay for editions
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                PostMedia(
                    post = post,
                    useFixedAspectRatio = false,
                    modifier = Modifier.fillMaxWidth()
                )

                // Price pill overlay for editions (top-right position like web)
                // Always show price, even if already purchased
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
        item {
            PostDetailActions(
                post = post,
                collectState = collectState,
                purchaseState = purchaseState,
                onLikeClick = onLikeClick,
                onCollectClick = onCollectClick,
                onPurchaseClick = onPurchaseClick
            )
        }

        // Caption
        if (!post.caption.isNullOrBlank()) {
            item {
                PostDetailCaption(
                    username = post.user.displayName ?: post.user.slug,
                    caption = post.caption,
                    onUserClick = { onUserClick(post.user.slug) },
                    onMentionClick = onUserClick
                )
            }
        }

        // Comments Section Header
        item {
            Spacer(modifier = Modifier.height(DesperseSpacing.lg))
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = DesperseSpacing.md)
            )
            Spacer(modifier = Modifier.height(DesperseSpacing.sm))
        }

        // Comments List
        if (isLoadingComments) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DesperseSpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        } else if (comments.isEmpty()) {
            item {
                Text(
                    text = "No comments yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = DesperseSpacing.md)
                )
            }
        } else {
            items(
                items = comments,
                key = { it.id }
            ) { comment ->
                SwipeableCommentItem(
                    comment = comment,
                    isOwnComment = comment.user.id == currentUserId,
                    isDeleting = deletingCommentId == comment.id,
                    onUserClick = { onUserClick(comment.user.slug) },
                    onMentionClick = { username -> onUserClick(username) },
                    onDelete = { onDeleteComment(comment) },
                    onReport = { onReportComment(comment) }
                )
            }
        }

        // Bottom spacing
        item {
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
        AsyncImage(
            model = post.user.avatarUrl,
            contentDescription = post.user.displayName ?: post.user.slug,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
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
                    text = "·",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatRelativeTime(post.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Type badge
                val typeInfo = getPostTypeInfo(post)
                if (typeInfo != null) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FaIcon(
                            icon = typeInfo.icon,
                            size = 10.dp,
                            tint = typeInfo.color
                        )
                        Text(
                            text = typeInfo.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeInfo.color
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostDetailActions(
    post: Post,
    collectState: CollectState,
    purchaseState: PurchaseState,
    onLikeClick: () -> Unit,
    onCollectClick: () -> Unit,
    onPurchaseClick: () -> Unit
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
                    tint = if (post.isLiked) DesperseTones.Like else MaterialTheme.colorScheme.onSurface
                )
                if (post.likeCount > 0) {
                    Text(
                        text = post.likeCount.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Comment count (read-only)
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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

        // Right side: Collect/Buy button based on post type
        when (post.type) {
            "collectible" -> {
                CollectButton(
                    collectCount = post.collectCount,
                    isCollected = post.isCollected,
                    collectState = collectState,
                    onClick = onCollectClick
                )
            }
            "edition" -> {
                BuyButton(
                    price = post.price,
                    currency = post.currency,
                    currentSupply = post.currentSupply ?: 0,
                    maxSupply = post.maxSupply,
                    isCollected = post.isCollected,
                    purchaseState = purchaseState,
                    onClick = onPurchaseClick
                )
            }
        }
    }
}

/**
 * Buy button for editions - COMPACT FORMAT
 * Matches web app design: icon + supply count only (price shown in media pill)
 * Icon: hexagon-image for 1/1, image-stack for limited/open editions
 */
@Composable
private fun BuyButton(
    price: Double?,
    currency: String?,
    currentSupply: Int,
    maxSupply: Int?,
    isCollected: Boolean,
    purchaseState: PurchaseState,
    onClick: () -> Unit
) {
    val toneColor = DesperseTones.Edition
    val isSoldOut = maxSupply != null && maxSupply > 0 && currentSupply >= maxSupply
    val isSuccess = isCollected || purchaseState is PurchaseState.Success
    val isFailed = purchaseState is PurchaseState.Failed
    val isLoading = purchaseState is PurchaseState.Preparing ||
            purchaseState is PurchaseState.Signing ||
            purchaseState is PurchaseState.Broadcasting ||
            purchaseState is PurchaseState.Submitting ||
            purchaseState is PurchaseState.Confirming

    // Supply count display: "2/5" for limited, "2" for open editions
    val supplyText = when {
        maxSupply == 1 -> "1/1"
        maxSupply != null && maxSupply > 0 -> "$currentSupply/$maxSupply"
        else -> formatCount(currentSupply)
    }

    // Icon: hexagon-image for 1/1, images for limited/open (same icon when owned, just solid)
    val icon = when {
        isFailed -> FaIcons.ArrowsRotate
        maxSupply == 1 -> FaIcons.HexagonImage
        else -> FaIcons.Images
    }

    // Color based on state
    val contentColor = when {
        isSuccess -> toneColor
        isFailed -> DesperseTones.Destructive
        isSoldOut -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val isClickable = !isSoldOut && !isLoading && !isSuccess

    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .then(if (isClickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = toneColor
            )
        } else {
            FaIcon(
                icon = icon,
                size = 18.dp,
                style = if (isSuccess) FaIconStyle.Solid else FaIconStyle.Regular,
                tint = contentColor
            )
        }
        // Always show supply count
        Text(
            text = supplyText,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )
    }
}

@Composable
private fun PostDetailCaption(
    username: String,
    caption: String,
    onUserClick: () -> Unit,
    onMentionClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.md, vertical = DesperseSpacing.sm)
    ) {
        Text(
            text = username,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onUserClick)
        )
        Spacer(modifier = Modifier.width(6.dp))
        MentionText(
            text = caption,
            onMentionClick = onMentionClick,
            style = MaterialTheme.typography.bodyMedium,
            textColor = MaterialTheme.colorScheme.onSurface
        )
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
private data class PostTypeInfo(
    val icon: String,
    val label: String,
    val color: androidx.compose.ui.graphics.Color
)

@Composable
private fun getPostTypeInfo(post: Post): PostTypeInfo? {
    return when (post.type) {
        "collectible" -> PostTypeInfo(
            icon = FaIcons.Gem,
            label = "Collectible",
            color = DesperseTones.Collectible
        )
        "edition" -> PostTypeInfo(
            icon = if (post.maxSupply == 1) FaIcons.Gem else FaIcons.Images,
            label = when {
                post.maxSupply == null || post.maxSupply == 0 -> "Open Edition"
                post.maxSupply == 1 -> "1/1"
                else -> "Limited Edition"
            },
            color = DesperseTones.Edition
        )
        else -> null
    }
}

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
 * Format price for display in pill
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
 * Format count (1.2k, 1.5M, etc.)
 */
private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
        count >= 1_000 -> "%.1fk".format(count / 1_000.0)
        else -> count.toString()
    }
}

/**
 * Format a timestamp string to relative time (e.g., "2h", "3d")
 */
private fun formatRelativeTime(timestamp: String): String {
    return try {
        val instant = Instant.parse(timestamp)
        val now = Instant.now()
        val seconds = ChronoUnit.SECONDS.between(instant, now)

        when {
            seconds < 60 -> "now"
            seconds < 3600 -> "${seconds / 60}m"
            seconds < 86400 -> "${seconds / 3600}h"
            seconds < 604800 -> "${seconds / 86400}d"
            seconds < 2592000 -> "${seconds / 604800}w"
            else -> {
                val date = java.time.LocalDate.ofInstant(instant, java.time.ZoneId.systemDefault())
                "${date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${date.dayOfMonth}"
            }
        }
    } catch (e: Exception) {
        ""
    }
}

/**
 * Comment input bar - Instagram-style design
 * Avatar on left, expandable input with send button inside (bottom-right)
 */
@Composable
private fun CommentInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSearch: suspend (String) -> List<MentionUser>,
    isSubmitting: Boolean,
    error: String?,
    onErrorDismiss: () -> Unit,
    avatarUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val maxLength = 280
    val isOverLimit = text.length > maxLength
    val canSubmit = text.trim().isNotEmpty() && !isOverLimit && !isSubmitting

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) {
        // Top border
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Error message (if any)
        if (error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = DesperseSpacing.md, vertical = DesperseSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onErrorDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    FaIcon(
                        icon = FaIcons.Xmark,
                        size = 14.dp,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Input row: Avatar + Input container with embedded send button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesperseSpacing.sm, vertical = DesperseSpacing.sm),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
        ) {
            // Avatar
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Your avatar",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            // Input container with send button inside
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(
                    1.dp,
                    if (isOverLimit) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Text field with @mention autocomplete
                    MentionTextField(
                        value = text,
                        onValueChange = onTextChange,
                        onSearch = onSearch,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 32.dp, max = 120.dp)
                            .padding(vertical = 6.dp),
                        placeholder = "Add a comment...",
                        enabled = !isSubmitting,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send,
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { if (canSubmit) onSubmit() }
                        )
                    )

                    // Send button - circular, anchored to bottom-right
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSubmit) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .then(
                                if (canSubmit && !isSubmitting)
                                    Modifier.clickable(onClick = onSubmit)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            FaIcon(
                                icon = FaIcons.ArrowUp,
                                size = 14.dp,
                                tint = if (canSubmit)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Over-limit warning (subtle, below the input)
        if (isOverLimit) {
            Text(
                text = "Comment must be $maxLength characters or less.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = DesperseSpacing.md, end = DesperseSpacing.md, bottom = DesperseSpacing.xs)
            )
        }
    }
}
