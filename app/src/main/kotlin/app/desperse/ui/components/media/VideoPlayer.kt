package app.desperse.ui.components.media

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
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
import app.desperse.ui.theme.DesperseMotion
import app.desperse.ui.theme.DesperseSpacing
import kotlinx.coroutines.delay

/**
 * Video player component with auto-play, looping, and mute toggle.
 *
 * Feed mode (useFixedAspectRatio = true): always-visible mute button bottom-right,
 * parent handles tap/double-tap gestures for navigation and like.
 *
 * Detail mode (useFixedAspectRatio = false): tap anywhere toggles mute with
 * centered indicator that fades in/out.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    coverUrl: String? = null,
    maxAspectRatio: Float = 1.25f,
    useFixedAspectRatio: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var showMuteIndicator by remember { mutableStateOf(false) }
    var muteIndicatorKey by remember { mutableStateOf(0) }
    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }
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

    // Track visibility to defer ExoPlayer creation until actually on screen
    var isVisible by remember { mutableStateOf(false) }

    // Deferred ExoPlayer - only created when the composable becomes visible
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    // Create ExoPlayer only when visible
    LaunchedEffect(isVisible) {
        if (isVisible && exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUrl))
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f // Start muted
                prepare()
            }
        }
    }

    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) exoPlayer?.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    // Update mute state
    LaunchedEffect(isMuted, exoPlayer) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    // Auto-hide mute indicator after brief flash
    LaunchedEffect(muteIndicatorKey) {
        if (showMuteIndicator) {
            delay(800)
            showMuteIndicator = false
        }
    }

    // Listen for video size changes (only update aspect ratio if not using fixed)
    LaunchedEffect(exoPlayer, useFixedAspectRatio) {
        val player = exoPlayer ?: return@LaunchedEffect
        player.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    // Only update dynamic ratio when not using fixed ratio
                    if (!useFixedAspectRatio) {
                        videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !isPlaying) {
                    // Auto-play when ready
                    player.play()
                    isPlaying = true
                }
            }
        })
    }

    // Remember cover image request to avoid recreation during recomposition
    val coverImageRequest = remember(coverUrl) {
        coverUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(ImageOptimization.getOptimizedUrlForContext(url, ImageContext.FEED_THUMBNAIL))
                .crossfade(DesperseMotion.crossfadeMs)
                .build()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(displayAspectRatio)
            .background(Color.Black)
            .onGloballyPositioned { isVisible = true },
        contentAlignment = Alignment.Center
    ) {
        // Video player - only render when player is ready
        val currentPlayer = exoPlayer
        if (currentPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = currentPlayer
                        useController = false
                        // Feed: zoom to fill (crop excess), Detail: fit to show full video
                        resizeMode = if (useFixedAspectRatio) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { playerView ->
                    playerView.player = currentPlayer
                    playerView.resizeMode = if (useFixedAspectRatio) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                }
            )
        }

        // Poster overlay when paused
        AnimatedVisibility(
            visible = !isPlaying && coverImageRequest != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AsyncImage(
                model = coverImageRequest,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = if (useFixedAspectRatio) ContentScale.Crop else ContentScale.Fit
            )
        }

        if (useFixedAspectRatio) {
            // Feed mode: always-visible mute button in bottom-right corner
            // Parent handles single-tap (detail) and double-tap (like)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(DesperseSpacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { isMuted = !isMuted },
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
        } else {
            // Detail mode: tap anywhere to toggle mute, centered indicator flashes
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isMuted = !isMuted
                        showMuteIndicator = true
                        muteIndicatorKey++
                    }
            )

            AnimatedVisibility(
                visible = showMuteIndicator,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    FaIcon(
                        icon = if (isMuted) FaIcons.VolumeMute else FaIcons.VolumeUp,
                        size = 24.dp,
                        tint = Color.White,
                        style = FaIconStyle.Solid,
                        contentDescription = if (isMuted) "Muted" else "Unmuted"
                    )
                }
            }
        }
    }
}
