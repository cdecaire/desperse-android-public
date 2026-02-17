package app.desperse.ui.components.media

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.desperse.data.model.Post
import app.desperse.data.model.PostAsset

/**
 * Main media display component that routes to appropriate renderer
 *
 * This component detects the media type and delegates to the appropriate
 * specialized component (image, video, audio, document, carousel).
 *
 * Logic:
 * - If assets list has >1 item: use MediaCarousel
 * - Otherwise detect type from mediaUrl and use appropriate component
 *
 * @param post The post containing media information
 * @param maxAspectRatio Maximum height/width ratio (default 1.25 = 4:5 for feed)
 * @param useFixedAspectRatio If true, use fixed 4:5 ratio to prevent layout shifts during scroll
 * @param onClick Click handler for opening detail view
 * @param modifier Modifier for the container
 */
@Composable
fun PostMedia(
    post: Post,
    maxAspectRatio: Float = 1.25f,
    useFixedAspectRatio: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Fixed ratio for stable scroll (4:5 = 0.8 width/height)
    val fixedRatio = if (useFixedAspectRatio) 1f / maxAspectRatio else null
    // Check if this is a multi-asset post
    val assets = post.assets
    if (assets != null && assets.size > 1) {
        MediaCarousel(
            assets = assets,
            coverUrl = post.coverUrl,
            maxAspectRatio = maxAspectRatio,
            useFixedAspectRatio = useFixedAspectRatio,
            onClick = { onClick() },
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    // Single asset post - detect media type from URL
    val mediaUrl = post.mediaUrl
    val coverUrl = post.coverUrl
    // Use mediaUrl if available, fallback to coverUrl for display
    val effectiveUrl = mediaUrl ?: coverUrl

    // If no URL available, don't render anything
    if (effectiveUrl == null) return

    val mediaType = remember(mediaUrl) { detectMediaType(mediaUrl) }

    when (mediaType) {
        MediaType.IMAGE -> {
            BlurredBackgroundImage(
                imageUrl = effectiveUrl,
                maxAspectRatio = maxAspectRatio,
                fixedAspectRatio = fixedRatio,
                contentDescription = "Post image",
                onClick = onClick,
                modifier = modifier.fillMaxWidth()
            )
        }

        MediaType.VIDEO -> {
            // Video needs the actual video URL, not cover
            if (mediaUrl != null) {
                VideoPlayer(
                    videoUrl = mediaUrl,
                    coverUrl = coverUrl,
                    maxAspectRatio = maxAspectRatio,
                    useFixedAspectRatio = useFixedAspectRatio,
                    onClick = onClick,
                    modifier = modifier.fillMaxWidth()
                )
            } else if (coverUrl != null) {
                // Fallback to showing cover as image
                BlurredBackgroundImage(
                    imageUrl = coverUrl,
                    maxAspectRatio = maxAspectRatio,
                    fixedAspectRatio = fixedRatio,
                    contentDescription = "Post cover",
                    onClick = onClick,
                    modifier = modifier.fillMaxWidth()
                )
            }
        }

        MediaType.AUDIO -> {
            // Audio needs the actual audio URL
            if (mediaUrl != null) {
                AudioPlayer(
                    audioUrl = mediaUrl,
                    coverUrl = coverUrl,
                    maxAspectRatio = maxAspectRatio,
                    onClick = onClick,
                    modifier = modifier.fillMaxWidth()
                )
            } else if (coverUrl != null) {
                // Fallback to showing cover as image
                BlurredBackgroundImage(
                    imageUrl = coverUrl,
                    maxAspectRatio = maxAspectRatio,
                    fixedAspectRatio = fixedRatio,
                    contentDescription = "Post cover",
                    onClick = onClick,
                    modifier = modifier.fillMaxWidth()
                )
            }
        }

        MediaType.DOCUMENT -> {
            DocumentPreview(
                documentUrl = mediaUrl ?: "",
                coverUrl = coverUrl,
                onClick = onClick,
                modifier = modifier.fillMaxWidth()
            )
        }

        MediaType.MODEL_3D -> {
            Model3DPreview(
                modelUrl = mediaUrl ?: "",
                coverUrl = coverUrl,
                onClick = onClick,
                modifier = modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Simplified media component for single URL (used in places without full Post object)
 *
 * @param mediaUrl URL of the media to display
 * @param coverUrl Optional cover image URL
 * @param maxAspectRatio Maximum height/width ratio
 * @param onClick Click handler
 * @param modifier Modifier for the container
 */
@Composable
fun SingleMedia(
    mediaUrl: String,
    coverUrl: String? = null,
    maxAspectRatio: Float = 1.25f,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val mediaType = remember(mediaUrl) { detectMediaType(mediaUrl) }

    when (mediaType) {
        MediaType.IMAGE -> {
            BlurredBackgroundImage(
                imageUrl = mediaUrl,
                maxAspectRatio = maxAspectRatio,
                contentDescription = "Image",
                onClick = onClick,
                modifier = modifier.fillMaxWidth()
            )
        }

        MediaType.VIDEO -> {
            VideoPlayer(
                videoUrl = mediaUrl,
                coverUrl = coverUrl,
                maxAspectRatio = maxAspectRatio,
                onClick = onClick,
                modifier = modifier.fillMaxWidth()
            )
        }

        MediaType.AUDIO -> {
            AudioPlayer(
                audioUrl = mediaUrl,
                coverUrl = coverUrl,
                maxAspectRatio = maxAspectRatio,
                onClick = onClick,
                modifier = modifier.fillMaxWidth()
            )
        }

        MediaType.DOCUMENT -> {
            DocumentPreview(
                documentUrl = mediaUrl,
                coverUrl = coverUrl,
                onClick = onClick,
                modifier = modifier.fillMaxWidth()
            )
        }

        MediaType.MODEL_3D -> {
            Model3DPreview(
                modelUrl = mediaUrl,
                coverUrl = coverUrl,
                onClick = onClick,
                modifier = modifier.fillMaxWidth()
            )
        }
    }
}
