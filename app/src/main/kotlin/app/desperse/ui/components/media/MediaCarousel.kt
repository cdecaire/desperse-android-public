package app.desperse.ui.components.media

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.desperse.data.model.PostAsset
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseSpacing

/**
 * Media carousel for multi-image/multi-asset posts
 *
 * Features:
 * - HorizontalPager for swipe navigation
 * - Dots indicator at bottom
 * - Support for mixed media types (image, video, audio)
 *
 * @param assets List of assets to display
 * @param coverUrl Cover URL for video/audio posts (used as poster)
 * @param maxAspectRatio Maximum height/width ratio for display
 * @param useFixedAspectRatio If true, use fixed ratio to prevent layout shifts during scroll
 * @param onClick Click handler for opening detail view
 * @param modifier Modifier for the container
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaCarousel(
    assets: List<PostAsset>,
    coverUrl: String? = null,
    maxAspectRatio: Float = 1.25f,
    useFixedAspectRatio: Boolean = true,
    onClick: ((index: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Fixed ratio for stable scroll (4:5 = 0.8 width/height)
    val fixedRatio = if (useFixedAspectRatio) 1f / maxAspectRatio else null
    if (assets.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { assets.size })

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val asset = assets[page]
            val mediaType = detectMediaTypeFromMime(asset.mimeType)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f / maxAspectRatio) // Use max aspect ratio for consistent carousel height
            ) {
                when (mediaType) {
                    MediaType.IMAGE -> {
                        BlurredBackgroundImage(
                            imageUrl = asset.url,
                            maxAspectRatio = maxAspectRatio,
                            fixedAspectRatio = fixedRatio,
                            contentDescription = "Image ${page + 1} of ${assets.size}",
                            onClick = onClick?.let { fn -> { fn(page) } },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    MediaType.VIDEO -> {
                        VideoPlayer(
                            videoUrl = asset.url,
                            coverUrl = coverUrl,
                            maxAspectRatio = maxAspectRatio,
                            useFixedAspectRatio = useFixedAspectRatio,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    MediaType.AUDIO -> {
                        AudioPlayer(
                            audioUrl = asset.url,
                            coverUrl = coverUrl,
                            useFixedAspectRatio = useFixedAspectRatio,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    MediaType.DOCUMENT -> {
                        DocumentPreview(
                            documentUrl = asset.url,
                            coverUrl = coverUrl,
                            onClick = onClick?.let { fn -> { fn(page) } },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    MediaType.MODEL_3D -> {
                        Model3DPreview(
                            modelUrl = asset.url,
                            coverUrl = coverUrl,
                            interactive = !useFixedAspectRatio,
                            onClick = onClick?.let { fn -> { fn(page) } },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Dots indicator
        if (assets.size > 1) {
            DotsIndicator(
                totalDots = assets.size,
                selectedIndex = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = DesperseSpacing.sm)
            )
        }

        // Multi-image badge (top left - to avoid conflict with price pill on top right)
        if (assets.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(DesperseSpacing.sm)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "${pagerState.currentPage + 1}/${assets.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Dots indicator for carousel
 */
@Composable
private fun DotsIndicator(
    totalDots: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == selectedIndex) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == selectedIndex) {
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.5f)
                        }
                    )
            )
        }
    }
}

/**
 * Document preview placeholder
 * Shows cover image with file type badge
 */
@Composable
fun DocumentPreview(
    documentUrl: String,
    coverUrl: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl != null) {
            BlurredBackgroundImage(
                imageUrl = coverUrl,
                maxAspectRatio = 1.25f,
                contentDescription = "Document cover",
                onClick = onClick
            )
        }

        // File type badge overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            FaIcon(
                icon = FaIcons.Download,
                size = 24.dp,
                tint = Color.White,
                style = FaIconStyle.Solid,
                contentDescription = "Download document"
            )
        }

    }
}

/**
 * 3D Model preview with two modes:
 *
 * Feed mode (interactive = false, default): Shows cover image with a centered
 * 3D cube badge. Tapping navigates to the post detail via [onClick].
 *
 * Detail mode (interactive = true): Renders the GLB model inline using Filament
 * with orbit/zoom/pan gestures. Falls back to cover image if model URL is missing.
 */
@Composable
fun Model3DPreview(
    modelUrl: String,
    coverUrl: String? = null,
    interactive: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // In interactive/detail mode, render the 3D viewer directly
    if (interactive && modelUrl.isNotBlank()) {
        ModelViewer(
            modelSource = ModelSource.Url(modelUrl),
            // Square aspect ratio so the viewer has bounded height in LazyColumn
            modifier = modifier.aspectRatio(1f)
        )
        return
    }

    // Feed mode: cover image + 3D badge overlay
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl != null) {
            BlurredBackgroundImage(
                imageUrl = coverUrl,
                maxAspectRatio = 1.25f,
                contentDescription = "3D model cover",
                onClick = onClick
            )
        }

        // 3D indicator badge (bottom right — matches video/audio mute button position)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(DesperseSpacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                FaIcon(
                    icon = FaIcons.Cube,
                    size = 16.dp,
                    tint = Color.White,
                    style = FaIconStyle.Solid,
                    contentDescription = "3D model"
                )
            }
        }
    }
}
