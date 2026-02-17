package app.desperse.ui.components.media

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseSpacing
import kotlinx.coroutines.delay

/**
 * Video player component with auto-play, mute toggle, and poster image support
 *
 * Features:
 * - Auto-play when visible (starts muted)
 * - Play/pause button overlay (center)
 * - Mute toggle (bottom right)
 * - Poster image (coverUrl) shown when paused
 * - Video indicator badge (top right)
 * - Blurred background support for portrait videos
 *
 * @param videoUrl URL of the video to play
 * @param coverUrl Optional cover image shown when paused
 * @param maxAspectRatio Maximum height/width ratio before applying blur treatment
 * @param useFixedAspectRatio If true, use fixed 4:5 ratio to prevent layout shifts during scroll
 * @param onClick Click handler for opening detail view
 * @param modifier Modifier for the container
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    coverUrl: String? = null,
    maxAspectRatio: Float = 1.25f,
    useFixedAspectRatio: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(false) }  // Start hidden
    var controlsKey by remember { mutableStateOf(0) }  // For resetting auto-hide timer
    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }
    var needsBlurredBg by remember { mutableStateOf(useFixedAspectRatio) }

    // Fixed ratio for stable scroll (4:5 = 0.8 width/height)
    val fixedRatio = 1f / maxAspectRatio

    // Calculate display aspect ratio - use fixed if enabled
    val displayAspectRatio = if (useFixedAspectRatio) {
        fixedRatio
    } else {
        remember(videoAspectRatio, maxAspectRatio) {
            val aspectRatioHeight = 1f / videoAspectRatio
            if (aspectRatioHeight > maxAspectRatio) {
                1f / maxAspectRatio
            } else {
                videoAspectRatio
            }
        }
    }

    // Create ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f // Start muted
            prepare()
        }
    }

    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) exoPlayer.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Update mute state
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Auto-hide controls after 3 seconds when playing
    LaunchedEffect(showControls, isPlaying, controlsKey) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    // Listen for video size changes (only update aspect ratio if not using fixed)
    LaunchedEffect(exoPlayer, useFixedAspectRatio) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    // Only update dynamic ratio when not using fixed ratio
                    if (!useFixedAspectRatio) {
                        videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                        needsBlurredBg = (videoSize.height.toFloat() / videoSize.width.toFloat()) > maxAspectRatio
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !isPlaying) {
                    // Auto-play when ready
                    exoPlayer.play()
                    isPlaying = true
                }
            }
        })
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(displayAspectRatio)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Blurred background for portrait videos
        if (needsBlurredBg && coverUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(ImageOptimization.getOptimizedUrlForContext(coverUrl, ImageContext.FEED_THUMBNAIL))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.1f)
                    .blur(24.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.6f
            )
        }

        // Video player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = if (needsBlurredBg) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                playerView.resizeMode = if (needsBlurredBg) {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            }
        )

        // Poster overlay when paused
        AnimatedVisibility(
            visible = !isPlaying && coverUrl != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(ImageOptimization.getOptimizedUrlForContext(coverUrl, ImageContext.FEED_THUMBNAIL))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (needsBlurredBg) ContentScale.Fit else ContentScale.Crop
                )
            }
        }

        // Tap overlay for toggling controls (doesn't block button clicks)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (showControls) {
                        // If controls visible, tap opens detail
                        onClick()
                    } else {
                        // If controls hidden, tap shows them
                        showControls = true
                        controlsKey++  // Reset auto-hide timer
                    }
                }
        )

        // Controls overlay
        AnimatedVisibility(
            visible = showControls || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Play/Pause button (center)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                            isPlaying = !isPlaying
                            controlsKey++  // Reset auto-hide timer
                        },
                    contentAlignment = Alignment.Center
                ) {
                    FaIcon(
                        icon = if (isPlaying) FaIcons.Pause else FaIcons.Play,
                        size = 24.dp,
                        tint = Color.White,
                        style = FaIconStyle.Solid,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }

                // Mute toggle (bottom right with gradient)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(DesperseSpacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                isMuted = !isMuted
                                controlsKey++  // Reset auto-hide timer
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        FaIcon(
                            icon = if (isMuted) FaIcons.VolumeMute else FaIcons.VolumeUp,
                            size = 16.dp,
                            tint = Color.White,
                            style = FaIconStyle.Solid,
                            contentDescription = if (isMuted) "Unmute" else "Mute"
                        )
                    }
                }
            }
        }
    }
}
