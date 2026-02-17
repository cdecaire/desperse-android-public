package app.desperse.ui.components.media

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import app.desperse.ui.theme.DesperseRadius
import app.desperse.ui.theme.DesperseSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Audio player component with cover image and controls overlay
 *
 * Features:
 * - Cover image fills container (or music icon placeholder)
 * - Audio controls bar at bottom (semi-transparent with blur)
 * - Play/pause button
 * - Progress bar with seek
 * - Time display (current / duration)
 * - Music indicator badge (top right)
 *
 * @param audioUrl URL of the audio to play
 * @param coverUrl Optional cover image
 * @param maxAspectRatio Maximum height/width ratio for display
 * @param onClick Click handler for opening detail view
 * @param modifier Modifier for the container
 */
@Composable
fun AudioPlayer(
    audioUrl: String,
    coverUrl: String? = null,
    maxAspectRatio: Float = 1.25f,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    // Use 1:1 aspect ratio for audio (like album art)
    val displayAspectRatio = 1f

    // Create ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(audioUrl))
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

    // Listen for playback state changes
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        })
    }

    // Update position while playing
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            if (!isSeeking) {
                currentPosition = exoPlayer.currentPosition
            }
            delay(100)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(displayAspectRatio)
            .clip(RoundedCornerShape(0.dp))
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
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
                contentScale = ContentScale.Crop
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

        // Music indicator badge (top right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(DesperseSpacing.sm)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            FaIcon(
                icon = FaIcons.Music,
                size = 12.dp,
                tint = Color.White,
                style = FaIconStyle.Solid,
                contentDescription = null
            )
        }

        // Audio controls bar (bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(DesperseSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesperseSpacing.sm)
            ) {
                // Play/Pause button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    FaIcon(
                        icon = if (isPlaying) FaIcons.Pause else FaIcons.Play,
                        size = 16.dp,
                        tint = Color.White,
                        style = FaIconStyle.Solid,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }

                // Progress section
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Progress slider
                    val progress = if (duration > 0) {
                        if (isSeeking) seekPosition else (currentPosition.toFloat() / duration.toFloat())
                    } else 0f

                    Slider(
                        value = progress,
                        onValueChange = { value ->
                            isSeeking = true
                            seekPosition = value
                        },
                        onValueChangeFinished = {
                            exoPlayer.seekTo((seekPosition * duration).toLong())
                            currentPosition = (seekPosition * duration).toLong()
                            isSeeking = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    // Time display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(if (isSeeking) (seekPosition * duration).toLong() else currentPosition),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = formatDuration(duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Format duration in milliseconds to MM:SS format
 */
private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
