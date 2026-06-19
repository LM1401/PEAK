package com.example.peak.ui.screens.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.example.peak.data.repository.ContinueWatchingRepository
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    movieId: String,
    onPlaybackFinished: () -> Unit,
    continueWatchingRepository: ContinueWatchingRepository,
    movieRepository: MovieRepository
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Obtain ViewModel via stable factory
    val viewModel: PlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = PlayerViewModelFactory(continueWatchingRepository, movieRepository)
    )

    // ISSUE 1 — REACTIVE STATE (NO BLOCKING .first())
    val videoUrl by viewModel.videoUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val resumePosition by viewModel.resumePosition.collectAsState()
    
    // STABLE EXOPLAYER INSTANCE
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // ISSUE 4 — STABILISE INITIALISATION (ONE TRIGGER PER MOVIEID)
    LaunchedEffect(movieId) {
        viewModel.loadMovie(movieId)
        
        // Wait for videoUrl reactively inside the coroutine without .first()
        viewModel.videoUrl.filterNotNull().collect { url ->
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            
            // Apply reactive resume position
            if (resumePosition > 0) {
                exoPlayer.seekTo(resumePosition)
            }
            
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            
            // Stop initialization collector to prevent double-init on URL change
            cancel()
        }
    }

    // PERIODIC PROGRESS UPDATES
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                val currentPos = exoPlayer.currentPosition
                val duration = exoPlayer.duration
                if (duration > 0) {
                    viewModel.updatePlaybackPosition(currentPos, duration)
                }
            }
            delay(5000)
        }
    }

    // ISSUE 3 — SINGLE EXIT STRATEGY (ON_PAUSE ONLY)
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            // Save on pause/stop (navigation back or app backgrounded)
            if (event == Lifecycle.Event.ON_PAUSE) {
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
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading && videoUrl == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading Video...", color = Color.White)
            }
        } else if (!isLoading && videoUrl == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Video not available", color = Color.Red)
            }
        } else {
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
}
