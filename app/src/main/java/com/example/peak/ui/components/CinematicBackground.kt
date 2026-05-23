package com.example.peak.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * A full-screen cinematic background component for Android TV.
 * Features a slow-zoom animation and dark gradient overlays.
 *
 * @param backdropUrl The URL of the image to display. If null, a fallback gradient is shown.
 */
@Composable
fun CinematicBackground(
    backdropUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
            .background(Color.Black) // Base layer
    ) {
        // 2. Image Layer with Crossfade for smooth URL transitions
        Crossfade(
            targetState = backdropUrl,
            animationSpec = tween(600), // Faster, snappier transition
            label = "BackdropCrossfade"
        ) { url ->
            if (url != null) {
                val request = remember(url) {
                    ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build()
                }

                AsyncImage(
                    model = request,
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
                // Fallback state when no URL is provided
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1A1A1A), Color.Black)
                            )
                        )
                )
            }
        }

        // 3. Cinematic Scrim Overlays
        // Vertical Gradient (Top Transparent -> Bottom Deep Black)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.5f to Color.Black.copy(alpha = 0.4f),
                        1.0f to Color.Black.copy(alpha = 0.8f)
                    )
                )
        )
    }
}
