package app.desperse.ui.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import app.desperse.ui.theme.DesperseMotion

/**
 * Image component with aspect ratio handling for feed and detail contexts.
 *
 * Feed mode (fixedAspectRatio != null):
 * - Fixed 4:5 container with ContentScale.Crop (fills edge-to-edge, crops excess)
 * - Matches Instagram/Twitter behavior for tall images in feeds
 *
 * Detail mode (fixedAspectRatio == null):
 * - Dynamic aspect ratio from image dimensions (capped at maxAspectRatio)
 * - ContentScale.Fit so users can see the full uncropped image
 *
 * @param imageUrl URL of the image to display
 * @param maxAspectRatio Maximum height/width ratio (default 1.25 = 4:5)
 * @param fixedAspectRatio If provided, use this fixed ratio (feed mode with crop-to-fill)
 * @param contentDescription Accessibility description
 * @param onClick Click handler
 * @param modifier Modifier for the container
 */
@Composable
fun BlurredBackgroundImage(
    imageUrl: String,
    maxAspectRatio: Float = 1.25f,
    fixedAspectRatio: Float? = null,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val minWidthHeightRatio = 1f / maxAspectRatio // 0.8 for 4:5

    // Use fixed aspect ratio if provided (feed: crop-to-fill), otherwise dynamic (detail: fit)
    val useFixedRatio = fixedAspectRatio != null
    val stableRatio = fixedAspectRatio ?: minWidthHeightRatio

    // State for dynamic sizing (only used when fixedAspectRatio is null)
    var displayRatio by remember(imageUrl) { mutableFloatStateOf(stableRatio) }

    // Actual ratio to use for layout
    val effectiveRatio = if (useFixedRatio) stableRatio else displayRatio

    // Optimized URL - computed once per imageUrl
    val optimizedUrl = remember(imageUrl) {
        ImageOptimization.getOptimizedUrlForContext(imageUrl, ImageContext.FEED_THUMBNAIL)
    }

    // ImageRequest - remembered to avoid recreation
    val imageRequest = remember(optimizedUrl) {
        ImageRequest.Builder(context)
            .data(optimizedUrl)
            .crossfade(DesperseMotion.crossfadeMs)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(effectiveRatio)
            .clipToBounds()
            .background(Color.Black)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center
    ) {
        // Feed: Crop to fill (edge-to-edge, crops excess)
        // Detail: Fit entire image (letterboxed with black bg)
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            filterQuality = FilterQuality.Low,
            contentScale = if (useFixedRatio) ContentScale.Crop else ContentScale.Fit,
            onState = { state ->
                // Only update dynamic ratio when not using fixed ratio
                if (!useFixedRatio && state is AsyncImagePainter.State.Success) {
                    val size = state.painter.intrinsicSize
                    if (size.width > 0 && size.height > 0) {
                        val imageRatio = size.width / size.height
                        val isTooTall = imageRatio < minWidthHeightRatio
                        val newRatio = if (isTooTall) minWidthHeightRatio else imageRatio
                        if (displayRatio != newRatio) {
                            displayRatio = newRatio
                        }
                    }
                }
            }
        )
    }
}

/**
 * Simple optimized image with fixed aspect ratio
 */
@Composable
fun OptimizedImage(
    imageUrl: String,
    contentDescription: String? = null,
    aspectRatio: Float = 1f,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    imageContext: ImageContext = ImageContext.FEED_THUMBNAIL
) {
    val context = LocalContext.current
    val optimizedUrl = remember(imageUrl, imageContext) {
        ImageOptimization.getOptimizedUrlForContext(imageUrl, imageContext)
    }

    val imageRequest = remember(optimizedUrl) {
        ImageRequest.Builder(context)
            .data(optimizedUrl)
            .crossfade(DesperseMotion.crossfadeMs)
            .build()
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clipToBounds()
            .clickable(onClick = onClick),
        filterQuality = FilterQuality.Low,
        contentScale = ContentScale.Crop
    )
}
