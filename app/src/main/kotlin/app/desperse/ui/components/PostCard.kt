package app.desperse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import app.desperse.data.model.CollectState
import app.desperse.data.model.Post
import app.desperse.data.model.PurchaseState
import app.desperse.ui.components.media.PostMedia
import app.desperse.ui.theme.DesperseComponentSpacing
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSizes
import app.desperse.ui.theme.DesperseSpacing
import app.desperse.ui.theme.DesperseTones
import app.desperse.ui.theme.colorOverlay
import app.desperse.ui.theme.toneCollectible
import app.desperse.ui.theme.toneEdition
import app.desperse.ui.theme.toneInfo
import app.desperse.ui.theme.toneLike
import app.desperse.ui.util.formatCount
import app.desperse.ui.util.formatRelativeTime
import app.desperse.ui.util.MintWindowPhase
import app.desperse.ui.util.MintWindowUtils

/**
 * Post Card Component - matches web app design
 *
 * Key design decisions from web:
 * - No card wrapper/border - posts flow directly in the feed
 * - Full-width media on mobile (no horizontal padding)
 * - Type label inline with metadata, not a separate badge
 * - Actions: Like + Comment on left, Collect/Buy on right
 */
@Composable
fun PostCard(
    post: Post,
    onClick: () -> Unit,
    onUserClick: () -> Unit,
    onMentionClick: (String) -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onCollectClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onReport: () -> Unit = {},
    onEditPost: () -> Unit = {},
    onDeletePost: () -> Unit = {},
    isOwnPost: Boolean = false,
    collectState: CollectState = CollectState.Idle,
    purchaseState: PurchaseState = PurchaseState.Idle,
    modifier: Modifier = Modifier
) {
    val mintPhase = remember(post.mintWindowStart, post.mintWindowEnd) {
        MintWindowUtils.getMintWindowPhase(post.mintWindowStart, post.mintWindowEnd)
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // User header with inline type label
        PostCardHeader(
            post = post,
            onUserClick = onUserClick,
            onPostClick = onClick,
            onReport = onReport,
            onEditPost = onEditPost,
            onDeletePost = onDeletePost,
            isOwnPost = isOwnPost,
            hasDownloadAccess = post.isCollected || isOwnPost
        )

        // Full-width media (double-tap to like)
        PostCardMedia(
            post = post,
            mintPhase = mintPhase,
            onClick = onClick,
            onDoubleTapLike = onLikeClick
        )

        // Actions row
        PostCardActions(
            post = post,
            mintPhase = mintPhase,
            onLikeClick = onLikeClick,
            onCommentClick = onCommentClick,
            onCollectClick = onCollectClick,
            onShareClick = onShareClick,
            collectState = collectState,
            purchaseState = purchaseState
        )

        // Caption
        if (!post.caption.isNullOrBlank()) {
            PostCardCaption(
                caption = post.caption,
                username = post.user.displayName ?: post.user.slug,
                onClick = onClick,
                onMentionClick = onMentionClick
            )
        }

        // Bottom spacing between posts
        Spacer(modifier = Modifier.height(DesperseSpacing.lg))
    }
}

/**
 * Get the edition label based on maxSupply and mint window
 * - null/0 → "Open Edition" / "Timed Edition"
 * - 1 → "1/1"
 * - >1 → "Limited Edition" / "Timed Edition"
 */
private fun getEditionLabel(maxSupply: Int?, hasMintWindow: Boolean = false): String {
    if (hasMintWindow) return "Timed Edition"
    return when {
        maxSupply == null || maxSupply == 0 -> "Open Edition"
        maxSupply == 1 -> "1/1"
        else -> "Limited Edition"
    }
}

/**
 * Get the appropriate icon for the post type
 */
private fun getPostTypeIcon(post: Post): String? {
    return when (post.type) {
        "collectible" -> FaIcons.Gem
        "edition" -> if (post.maxSupply == 1) FaIcons.Gem else FaIcons.Images
        else -> null
    }
}

/**
 * Get the type label for display
 */
private fun getPostTypeLabel(post: Post): String? {
    val hasMintWindow = post.mintWindowStart != null && post.mintWindowEnd != null
    return when (post.type) {
        "collectible" -> "Collectible"
        "edition" -> getEditionLabel(post.maxSupply, hasMintWindow)
        else -> null
    }
}

/**
 * Post card header with user info and inline type label
 */
@Composable
private fun PostCardHeader(
    post: Post,
    onUserClick: () -> Unit,
    onPostClick: () -> Unit,
    onReport: () -> Unit,
    onEditPost: () -> Unit = {},
    onDeletePost: () -> Unit = {},
    isOwnPost: Boolean = false,
    hasDownloadAccess: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    val typeLabel = getPostTypeLabel(post)
    val typeIcon = getPostTypeIcon(post)
    val typeTone = when (post.type) {
        "collectible" -> toneCollectible()
        "edition" -> toneEdition()
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick() }
            .padding(
                horizontal = DesperseSpacing.md,
                vertical = DesperseSpacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        DesperseAvatar(
            imageUrl = post.user.avatarUrl,
            contentDescription = "${post.user.displayName ?: post.user.slug}'s avatar",
            identityInput = post.user.walletAddress ?: post.user.slug,
            size = AvatarSize.Medium
        )

        Spacer(modifier = Modifier.width(DesperseComponentSpacing.postCardAvatarGap))

        // Name and metadata row
        Column(modifier = Modifier.weight(1f)) {
            // Display name with optional verified badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.user.displayName ?: post.user.slug,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Username · time · type label (inline)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
            ) {
                Text(
                    text = "@${post.user.slug}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

                // Inline type label with icon
                if (typeLabel != null && typeTone != null) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (typeIcon != null) {
                        FaIcon(
                            icon = typeIcon,
                            size = 12.dp,
                            tint = typeTone,
                            style = FaIconStyle.Solid,
                            contentDescription = typeLabel
                        )
                    }
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = typeTone
                    )
                }

                // Arweave permanent storage badge
                if (post.storageType == "arweave") {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FaIcon(
                        icon = FaIcons.Database,
                        size = 10.dp,
                        tint = toneInfo(),
                        style = FaIconStyle.Solid,
                        contentDescription = "Permanent storage"
                    )
                }
            }
        }

        // Menu button
        DesperseFaIconButton(
            icon = FaIcons.EllipsisVertical,
            onClick = { showMenu = true },
            contentDescription = "More options",
            variant = ButtonVariant.Ghost
        )

        // Post menu sheet
        PostCardMenuSheet(
            isOpen = showMenu,
            post = post,
            onDismiss = { showMenu = false },
            onGoToPost = onPostClick,
            onReport = {
                showMenu = false
                onReport()
            },
            onEditPost = {
                showMenu = false
                onEditPost()
            },
            onDeletePost = {
                showMenu = false
                onDeletePost()
            },
            isOwnPost = isOwnPost,
            hasDownloadAccess = hasDownloadAccess
        )
    }
}

/**
 * Post card media section - full width, no horizontal padding
 * Uses PostMedia component for proper handling of different media types
 * Includes price pill overlay for editions
 */
@Composable
private fun PostCardMedia(
    post: Post,
    mintPhase: MintWindowPhase,
    onClick: () -> Unit,
    onDoubleTapLike: () -> Unit = {}
) {
    var showHeartAnimation by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }
    // Particle burst: progress drives position (0→1 = center→outward) and fade
    val particleProgress = remember { Animatable(0f) }
    val likeColor = toneLike()

    // Stable particle angles (radial burst, evenly distributed with slight randomness)
    val particleAngles = remember {
        val base = List(8) { i -> (i * 45.0) + (i * 7.0 - 24.0) } // 8 particles, ~45° apart, slight offset
        base.map { Math.toRadians(it) }
    }

    LaunchedEffect(showHeartAnimation) {
        if (showHeartAnimation) {
            // Reset all
            heartAlpha.snapTo(1f)
            heartScale.snapTo(0f)
            particleProgress.snapTo(0f)

            // Phase 1: Heart pops in with elastic overshoot
            heartScale.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = 0.4f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )

            // Phase 2: Particle burst + heart fade out in parallel
            coroutineScope {
                // Particles fly outward and fade
                launch {
                    particleProgress.animateTo(
                        1f,
                        animationSpec = tween(
                            durationMillis = 500,
                            easing = androidx.compose.animation.core.EaseOutCubic
                        )
                    )
                }
                // Heart shrinks + fades after brief hold
                launch {
                    delay(80)
                    coroutineScope {
                        launch {
                            heartScale.animateTo(0.7f, animationSpec = tween(260))
                        }
                        launch {
                            heartAlpha.animateTo(0f, animationSpec = tween(260))
                        }
                    }
                }
            }

            heartScale.snapTo(0f)
            particleProgress.snapTo(0f)
            showHeartAnimation = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = {
                        if (!post.isLiked) onDoubleTapLike()
                        showHeartAnimation = true
                    }
                )
            }
    ) {
        PostMedia(
            post = post,
            maxAspectRatio = 1.25f, // 4:5 max in feed
            onClick = null // handled by parent gesture detector
        )

        // Price pill overlay for editions (top-right position like web)
        // Prepend mint window status for timed editions
        if (post.type == "edition") {
            val basePrice = formatPriceText(post.price, post.currency)
            val priceText = if (mintPhase is MintWindowPhase.None) basePrice else "${mintPhase.label} · $basePrice"
            MediaPill(
                text = priceText,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = DesperseSpacing.lg, top = DesperseSpacing.sm)
            )
        }

        // Double-tap heart animation overlay
        if (showHeartAnimation || heartAlpha.value > 0f || particleProgress.value > 0f) {
            // Particle burst — small circles radiating outward from center
            val pProgress = particleProgress.value
            if (pProgress > 0f) {
                val particleColor = likeColor
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxRadius = size.minDimension * 0.35f
                    // Particles fade out in second half of progress
                    val particleAlpha = if (pProgress < 0.5f) 1f else 1f - ((pProgress - 0.5f) / 0.5f)
                    val particleSize = 4.dp.toPx() * (1f - pProgress * 0.6f) // Shrink as they fly

                    particleAngles.forEachIndexed { i, angle ->
                        val dist = maxRadius * pProgress
                        val px = cx + (dist * kotlin.math.cos(angle)).toFloat()
                        val py = cy + (dist * kotlin.math.sin(angle)).toFloat()
                        // Alternate between larger and smaller particles
                        val sizeMult = if (i % 2 == 0) 1f else 0.6f
                        drawCircle(
                            color = if (i % 3 == 0) Color.White else particleColor,
                            radius = particleSize * sizeMult,
                            center = Offset(px, py),
                            alpha = particleAlpha
                        )
                    }
                }
            }

            // Main heart
            FaIcon(
                icon = FaIcons.Heart,
                size = 80.dp,
                tint = likeColor,
                style = FaIconStyle.Solid,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .scale(heartScale.value)
                    .alpha(heartAlpha.value)
            )
        }
    }
}

/**
 * Price pill overlay on media - matches web app MediaPill component
 * Style: small rounded pill with backdrop blur effect
 */
@Composable
private fun MediaPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(DesperseRadius.md))
            .background(colorOverlay())
            .padding(horizontal = DesperseSpacing.sm, vertical = DesperseSpacing.xs)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
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
 * Post card actions row
 * Layout: [Like] [Comment] ... [Collect/Buy]
 */
@Composable
private fun PostCardActions(
    post: Post,
    mintPhase: MintWindowPhase = MintWindowPhase.None,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onCollectClick: () -> Unit,
    onShareClick: () -> Unit,
    collectState: CollectState = CollectState.Idle,
    purchaseState: PurchaseState = PurchaseState.Idle
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = DesperseSpacing.md,
                vertical = DesperseSpacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Like + Comment
        Row(
            horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like button
            ActionButton(
                icon = FaIcons.Heart,
                count = post.likeCount,
                isActive = post.isLiked,
                activeColor = toneLike(),
                onClick = onLikeClick,
                contentDescription = if (post.isLiked) "Unlike" else "Like",
                style = if (post.isLiked) FaIconStyle.Solid else FaIconStyle.Regular
            )

            // Comment button
            ActionButton(
                icon = FaIcons.Comment,
                count = post.commentCount,
                onClick = onCommentClick,
                contentDescription = "Comment",
                style = FaIconStyle.Regular
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Right side: Collect/Buy button (only for collectibles and editions)
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
                    mintPhase = mintPhase,
                    onClick = onCollectClick
                )
            }
            // Regular posts don't have an action button on the right
            // Share/Copy link is available via the menu (3-dot button)
        }
    }
}

/**
 * Generic action button with icon and count
 */
@Composable
private fun ActionButton(
    icon: String,
    count: Int?,
    onClick: () -> Unit,
    contentDescription: String,
    isActive: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    style: FaIconStyle = FaIconStyle.Solid
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(DesperseRadius.sm))
            .clickable { onClick() }
            .padding(DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
    ) {
        FaIcon(
            icon = icon,
            size = DesperseSizes.iconMd,
            tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            style = style,
            contentDescription = contentDescription
        )
        if (count != null && count > 0) {
            Text(
                text = formatCount(count),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Buy button for paid editions - COMPACT FORMAT
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
    purchaseState: PurchaseState = PurchaseState.Idle,
    mintPhase: MintWindowPhase = MintWindowPhase.None,
    onClick: () -> Unit
) {
    val toneColor = toneEdition()
    val isSoldOut = maxSupply != null && maxSupply > 0 && currentSupply >= maxSupply
    val isMintWindowBlocked = mintPhase is MintWindowPhase.Scheduled || mintPhase is MintWindowPhase.Ended

    // Check purchase flow states
    val isInProgress = purchaseState is PurchaseState.Preparing ||
            purchaseState is PurchaseState.Signing ||
            purchaseState is PurchaseState.Broadcasting ||
            purchaseState is PurchaseState.Submitting ||
            purchaseState is PurchaseState.Confirming
    val isSuccess = purchaseState is PurchaseState.Success
    val isFailed = purchaseState is PurchaseState.Failed
    val isOwned = isCollected || isSuccess

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
        isOwned -> toneColor
        isFailed -> MaterialTheme.colorScheme.error
        isSoldOut || isMintWindowBlocked -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val isClickable = !isSoldOut && !isOwned && !isInProgress && !isMintWindowBlocked

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(DesperseRadius.sm))
            .clickable(enabled = isClickable) { onClick() }
            .padding(DesperseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.xs)
    ) {
        when {
            isInProgress -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(DesperseSizes.iconMd),
                    strokeWidth = 2.dp,
                    color = toneColor
                )
            }
            else -> {
                FaIcon(
                    icon = icon,
                    size = DesperseSizes.iconMd,
                    tint = contentColor,
                    style = if (isOwned) FaIconStyle.Solid else FaIconStyle.Regular,
                    contentDescription = when {
                        isOwned -> "Owned"
                        isSoldOut -> "Sold out"
                        else -> "Buy"
                    }
                )
            }
        }
        // Always show supply count
        Text(
            text = supplyText,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )
    }
}

/**
 * Post card caption
 */
@Composable
private fun PostCardCaption(
    caption: String,
    username: String,
    onClick: () -> Unit,
    onMentionClick: (String) -> Unit,
    maxLines: Int = 3
) {
    val showMore = caption.length > 150

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesperseSpacing.md)
    ) {
        MentionText(
            text = caption,
            onMentionClick = onMentionClick,
            style = MaterialTheme.typography.bodyMedium,
            textColor = MaterialTheme.colorScheme.onBackground,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )

        if (showMore) {
            Text(
                text = "more",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onClick() }
            )
        }
    }
}

/**
 * Format relative time (simplified)
 */
