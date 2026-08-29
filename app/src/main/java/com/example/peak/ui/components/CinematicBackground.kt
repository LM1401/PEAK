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

/**
 * A full-screen cinematic background component for Android TV.
 * Optimized for zero-lag replacement during D-pad navigation.
 */
@Composable
fun CinematicBackground(
    focusedMovieProvider: @Composable () -> Movie?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val movie = focusedMovieProvider()
    val backdropUrl = movie?.backdropUrl
    val movieId = movie?.movieId

    // 1. Scale Animation (Slow zoom effect)
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
        // 2. HARD REPLACEMENT RENDERING
        key(movieId) {
            if (backdropUrl != null) {
                AsyncImage(
                    model = remember(movieId) {
                        ImageRequest.Builder(context)
                            .data(backdropUrl)
                            .memoryCacheKey("${movieId}_backdrop")
                            .diskCacheKey("${movieId}_backdrop")
                            .crossfade(false)
                            .allowHardware(true)
                            .build()
                    },
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
        }
    }
}
