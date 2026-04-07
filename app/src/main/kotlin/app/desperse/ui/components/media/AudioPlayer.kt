package app.desperse.ui.components.media

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseSpacing
import kotlinx.coroutines.delay

/**
 * Audio player component — mirrors VideoPlayer UX.
 *
 * Feed mode (useFixedAspectRatio = true): cover image, auto-plays muted and loops.
 * Mute/unmute button bottom-right (same as video). No top-right badge.
 * Parent handles tap/double-tap gestures for navigation and like.
 *
 * Detail mode (useFixedAspectRatio = false): cover image, auto-plays muted.
 * Tap anywhere toggles mute with centered indicator flash (same as video).
 * No scrubber/timeline.
 */
@Composable
fun AudioPlayer(
    audioUrl: String,
    coverUrl: String? = null,
    useFixedAspectRatio: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var showMuteIndicator by remember { mutableStateOf(false) }
    var muteIndicatorKey by remember { mutableStateOf(0) }

    // 1:1 aspect ratio for audio (like album art)
    val displayAspectRatio = 1f

    // Create ExoPlayer — same setup as original, just muted + looping
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(audioUrl))
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

    // Auto-play when ready
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !isPlaying) {
                    exoPlayer.play()
                    isPlaying = true
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        })
    }

    // Update mute state — re-apply whenever isMuted changes
    LaunchedEffect(isMuted, exoPlayer) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Auto-hide mute indicator after brief flash
    LaunchedEffect(muteIndicatorKey) {
        if (showMuteIndicator) {
            delay(800)
            showMuteIndicator = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(displayAspectRatio)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Cover image or placeholder
        if (coverUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(ImageOptimization.getOptimizedUrlForContext(coverUrl, ImageContext.COVER))
                    .crossfade(true)
                    .build(),
                contentDescription = "Audio cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low
            )
        } else {
            // Placeholder with music icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                FaIcon(
                    icon = FaIcons.Music,
                    size = 64.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    style = FaIconStyle.Solid,
                    contentDescription = null
                )
            }
        }

        if (useFixedAspectRatio) {
            // Feed mode: mute/unmute button bottom-right (same as video)
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
