package com.example.peak.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.peak.domain.model.Movie

import com.example.peak.ui.image.PeakImageLoader

private data class BackdropState(
    val key: String,
    val url: String
)

/**
 * A full-screen cinematic background component for Android TV.
 * Optimized for zero-lag replacement and smooth backdrop retention during D-pad navigation.
 */
@Composable
fun CinematicBackground(
    focusedMovieProvider: @Composable () -> Movie?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = remember { PeakImageLoader.getInstance(context) }
    val movie = focusedMovieProvider()
    val backdropUrl = movie?.backdropUrl
    val movieId = movie?.movieId
    val mediaType = movie?.mediaType

    val activeKey = if (movieId != null && mediaType != null && !backdropUrl.isNullOrBlank()) {
        "${mediaType.name}_${movieId}_backdrop"
    } else null

    var displayedBackdrop by remember { mutableStateOf<BackdropState?>(null) }
    var pendingBackdrop by remember { mutableStateOf<BackdropState?>(null) }

    LaunchedEffect(activeKey, backdropUrl) {
        if (activeKey != null && !backdropUrl.isNullOrBlank()) {
            if (displayedBackdrop?.key != activeKey) {
                pendingBackdrop = BackdropState(activeKey, backdropUrl)
            }
        } else {
            pendingBackdrop = null
            displayedBackdrop = null
        }
    }

    // Scale Animation (Slow zoom effect)
    val infiniteTransition = rememberInfiniteTransition(label = "CinematicZoom")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BackgroundScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF02060C)) // Deep Navy Base
    ) {
        // LAYER 1: Currently Confirmed Backdrop or Navy Gradient Fallback
        val current = displayedBackdrop
        if (current != null) {
            AsyncImage(
                model = remember(current.key, current.url) {
                    ImageRequest.Builder(context)
                        .data(current.url)
                        .memoryCacheKey(current.key)
                        .diskCacheKey(current.key)
                        .crossfade(false)
                        .allowHardware(true)
                        .build()
                },
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0B1729), Color(0xFF02060C))
                        )
                    )
            )
        }

        // LAYER 2: Pending Incoming Backdrop (Fades over Layer 1 when loaded)
        val pending = pendingBackdrop
        if (pending != null && pending.key != current?.key) {
            AsyncImage(
                model = remember(pending.key, pending.url) {
                    ImageRequest.Builder(context)
                        .data(pending.url)
                        .memoryCacheKey(pending.key)
                        .diskCacheKey(pending.key)
                        .crossfade(200)
                        .allowHardware(true)
                        .build()
                },
                imageLoader = imageLoader,
                contentDescription = null,
                onSuccess = {
                    if (pendingBackdrop?.key == pending.key) {
                        displayedBackdrop = pending
                        pendingBackdrop = null
                    }
                },
                onError = {
                    if (pendingBackdrop?.key == pending.key) {
                        pendingBackdrop = null
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentScale = ContentScale.Crop
            )
        }
    }
}
