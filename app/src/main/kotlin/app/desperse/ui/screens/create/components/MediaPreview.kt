package app.desperse.ui.screens.create.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.desperse.data.repository.DeviceMediaItem
import app.desperse.ui.screens.create.MediaTab
import app.desperse.ui.screens.create.UploadedMediaItem
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Preview area for the media selection screen.
 * Behavior varies by media tab:
 * - Image: swipeable pager of selected images
 * - Video: single video preview
 * - Audio: delegated to AudioPreview (cover + audio overlay)
 * - 3D: not used (ThreeDPickerView takes full screen)
 */
@OptIn(UnstableApi::class)
@Composable
fun MediaPreview(
    mediaTab: MediaTab,
    selectedItems: List<DeviceMediaItem>,
    audioFile: UploadedMediaItem? = null,
    threeDFile: UploadedMediaItem? = null,
    onAddAudio: () -> Unit = {},
    onRemoveAudio: () -> Unit = {},
    onAddThreeD: () -> Unit = {},
    onRemoveThreeD: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (mediaTab) {
            MediaTab.Image -> ImagePreview(selectedItems)
            MediaTab.Video -> {
                val selected = selectedItems.lastOrNull()
                if (selected == null) {
                    PlaceholderText("Select a video")
                } else {
                    VideoPreview(uri = selected.uri)
                }
            }
            MediaTab.Audio -> {
                AudioPreview(
                    coverItem = selectedItems.firstOrNull(),
                    audioFile = audioFile,
                    onAddAudio = onAddAudio,
                    onRemoveAudio = onRemoveAudio,
                    modifier = Modifier.fillMaxSize()
                )
            }
            MediaTab.ThreeD -> {
                ThreeDPreview(
                    coverItem = selectedItems.firstOrNull(),
                    threeDFile = threeDFile,
                    onAddThreeD = onAddThreeD,
                    onRemoveThreeD = onRemoveThreeD,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun PlaceholderText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.5f)
    )
}

@Composable
private fun ImagePreview(selectedItems: List<DeviceMediaItem>) {
    when {
        selectedItems.isEmpty() -> PlaceholderText("Select photos")
        selectedItems.size == 1 -> {
            val context = LocalContext.current
            val item = selectedItems.first()
            AsyncImage(
                model = remember(item.uri) {
                    ImageRequest.Builder(context)
                        .data(item.uri)
                        .crossfade(true)
                        .build()
                },
                contentDescription = "Selected image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        else -> {
            val pagerState = rememberPagerState(
                initialPage = selectedItems.size - 1,
                pageCount = { selectedItems.size }
            )
            // Jump to latest when selection changes
            LaunchedEffect(selectedItems.size) {
                pagerState.animateScrollToPage(selectedItems.size - 1)
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val context = LocalContext.current
                val item = selectedItems[page]
                AsyncImage(
                    model = remember(item.uri) {
                        ImageRequest.Builder(context)
                            .data(item.uri)
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = "Selected image ${page + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPreview(uri: android.net.Uri) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
        }
    }

    // Update media item when URI changes
    LaunchedEffect(uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
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
        })
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
