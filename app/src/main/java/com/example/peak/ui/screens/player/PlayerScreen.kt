package com.example.peak.ui.screens.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.peak.data.repository.ContinueWatchingRepository

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    movieId: String,
    videoUrl: String,
    onPlaybackFinished: () -> Unit,
    continueWatchingRepository: ContinueWatchingRepository? = null
) {
    val context = LocalContext.current

    // Obtain the ViewModel using the factory
    val viewModel: PlayerViewModel = if (continueWatchingRepository != null) {
        androidx.lifecycle.viewmodel.compose.viewModel(
            factory = PlayerViewModelFactory(continueWatchingRepository)
        )
    } else {
        androidx.lifecycle.viewmodel.compose.viewModel()
    }

    val resumePosition by viewModel.resumePosition.collectAsState()
    var isInitialSeekDone by remember { mutableStateOf(false) }

    // Initialize the playback session in the ViewModel
    LaunchedEffect(movieId) {
        viewModel.loadMovie(movieId)
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    // Handle Resume Logic: Seek to saved position once
    LaunchedEffect(resumePosition) {
        if (resumePosition > 0 && !isInitialSeekDone) {
            exoPlayer.seekTo(resumePosition)
            isInitialSeekDone = true
        }
    }

    // PERIODIC PROGRESS UPDATES: Polls ExoPlayer every 5s and sends to ViewModel
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                val currentPos = exoPlayer.currentPosition
                val duration = exoPlayer.duration
                if (duration > 0) {
                    viewModel.updatePlaybackPosition(currentPos, duration)
                }
            }
            kotlinx.coroutines.delay(5000)
        }
    }

    // Handle lifecycle: backgrounding and disposal
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            // Save on pause/stop (app backgrounded or screen covered)
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                val currentPos = exoPlayer.currentPosition
                val duration = exoPlayer.duration
                if (duration > 0) {
                    viewModel.onPlaybackStopped(currentPos, duration)
                }
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // FINAL SAVE: Capture exact position on exit (navigation back)
            val currentPos = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            if (duration > 0) {
                viewModel.onPlaybackStopped(currentPos, duration)
            }
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
