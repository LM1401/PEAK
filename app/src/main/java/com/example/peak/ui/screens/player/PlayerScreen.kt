package com.example.peak.ui.screens.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.example.peak.data.repository.ContinueWatchingRepository
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.player.CachedMediaSourceFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalTvMaterial3Api::class, UnstableApi::class)
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
    val errorMessage by viewModel.errorMessage.collectAsState()
    val errorFocusRequester = remember { FocusRequester() }

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
        
        if (session.headers.isNotEmpty()) {
            val customMediaSource = CachedMediaSourceFactory.createMediaSource(
                context = context,
                streamUrl = session.videoUrl,
                headers = session.headers
            )
            exoPlayer.setMediaSource(customMediaSource)
        } else {
            val mediaItem = MediaItem.fromUri(session.videoUrl)
            exoPlayer.setMediaItem(mediaItem)
        }
        
        if (session.resumePosition > 0) {
            exoPlayer.seekTo(session.resumePosition)
        }
        
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // EVENT LISTENERS: Completion & Errors
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.onPlaybackStopped(exoPlayer.duration, exoPlayer.duration)
                    onPlaybackFinished()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val reason = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "HTTP stream error (link may be forbidden or expired)."
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Network connection failed during playback."
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "Unsupported video codec or audio format."
                    else -> error.localizedMessage ?: "Playback error occurred."
                }
                viewModel.setPlaybackError("Playback Failed: $reason")
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

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            errorFocusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 500.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(28.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Playback Unsupported",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = errorMessage ?: "Unable to render video stream.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = onPlaybackFinished,
                                modifier = Modifier.focusRequester(errorFocusRequester),
                                colors = ButtonDefaults.colors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black,
                                    focusedContainerColor = Color.White,
                                    focusedContentColor = Color.Black
                                ),
                                scale = ButtonDefaults.scale(focusedScale = 1.05f),
                                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Back to Streams",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading Content...", color = Color.White)
                }
            }
            else -> {
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
}
