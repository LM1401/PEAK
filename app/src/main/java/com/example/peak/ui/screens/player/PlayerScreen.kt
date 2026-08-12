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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.example.peak.data.repository.ContinueWatchingRepository
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.domain.model.MediaType
import com.example.peak.player.CachedMediaSourceFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    mediaId: String,
    mediaType: MediaType,
    onPlaybackFinished: () -> Unit,
    continueWatchingRepository: ContinueWatchingRepository,
    movieRepository: MovieRepository
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: PlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = PlayerViewModelFactory(continueWatchingRepository, movieRepository)
    )

    val isLoading by viewModel.isLoading.collectAsState()
    
    // STABLE EXOPLAYER INSTANCE
    val exoPlayer = remember {
        val mediaSourceFactory = CachedMediaSourceFactory.getInstance(context)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    // DETERMINISTIC INITIALIZATION
    LaunchedEffect(mediaId, mediaType) {
        viewModel.loadMedia(mediaId, mediaType)
        
        // One-shot synchronization for playback session
        val session = viewModel.playbackSession.filterNotNull().first()
        
        val mediaItem = MediaItem.fromUri(session.videoUrl)
        exoPlayer.setMediaItem(mediaItem)
        
        if (session.resumePosition > 0) {
            exoPlayer.seekTo(session.resumePosition)
        }
        
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // EVENT LISTENERS: Natural completion
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.onPlaybackStopped(exoPlayer.duration, exoPlayer.duration)
                    onPlaybackFinished()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
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

    // LIFECYCLE: FINAL SAVE & CLEANUP
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading Content...", color = Color.White)
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
