package com.example.peak.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.peak.domain.model.Movie
import com.example.peak.ui.image.PeakImageLoader

import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Large, cinematic Movie Card for the Home Screen.
 * Optimized for performance: asynchronous loading with stable placeholders.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieCard(
    movie: Movie,
    onFocus: (Movie?) -> Unit,
    onClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val context = LocalContext.current
    val imageLoader = remember { PeakImageLoader.getInstance(context) }

    // LOCAL INTERACTION STATE: Zero-latency focus tracking
    var isLocalFocused by remember { mutableStateOf(false) }

    // GPU-ACCELERATED SCALE: Subtle pop-out effect
    val scale by animateFloatAsState(
        targetValue = if (isLocalFocused) 1.05f else 1f,
        label = "movie_card_scale"
    )

    Card(
        onClick = { onClick(movie) },
        modifier = modifier
            .onFocusChanged { state ->
                isLocalFocused = state.isFocused
                if (state.isFocused) {
                    onFocus(movie)
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin.Center
                clip = false
            },
        scale = CardDefaults.scale(focusedScale = 1f), // Disable native scaling
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)), // Slightly rounder for Netflix look
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.8f)),
                inset = 0.dp
            )
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(
                elevationColor = Color.Black.copy(alpha = 0.5f),
                elevation = 20.dp
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. BASE PLACEHOLDER
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
                ShimmerBox()
            }

            // 2. IMAGE SWAP (Synchronised with local focus for zero-lag response)
            val cacheKey = "${movie.mediaType.name}_${movie.movieId}"
            val posterKey = cacheKey
            val backdropKey = "${cacheKey}_backdrop"
            val isUsingBackdrop = isLocalFocused && movie.backdropUrl.isNotBlank()

            val imageRequest = remember<ImageRequest>(
                movie.movieId,
                movie.mediaType,
                movie.imageUrl,
                movie.backdropUrl,
                isLocalFocused
            ) {
                ImageRequest.Builder(context)
                    .data(if (isUsingBackdrop) movie.backdropUrl else movie.imageUrl)
                    .memoryCacheKey(if (isUsingBackdrop) backdropKey else posterKey)
                    .diskCacheKey(if (isUsingBackdrop) backdropKey else posterKey)
                    .crossfade(200)
                    .allowHardware(true)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                imageLoader = imageLoader,
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 3. PROGRESS BAR OVERLAY (At the very bottom)
            if (progress != null && progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.Gray.copy(alpha = 0.5f))
                        .align(Alignment.BottomStart)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color.Red)
                    )
                }
            }
        }
    }
}

/**
 * Compact, stable Movie Card for the Detail Screen.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailMovieCard(
    movie: Movie, 
    onMovieFocused: (Movie?) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(movie.movieId, movie.mediaType) {
        val cacheKey = "${movie.mediaType.name}_${movie.movieId}"
        ImageRequest.Builder(context)
            .data(movie.imageUrl)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .crossfade(false)
            .allowHardware(true)
            .build()
    }

    Card(
        onClick = { onMovieClick(movie) },
        modifier = Modifier
            .width(135.dp)
            .aspectRatio(1.8f / 3f)
            .padding(6.dp)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    onMovieFocused(movie)
                }
            },
        scale = CardDefaults.scale(focusedScale = 1.03f),
        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                inset = 0.dp
            )
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(
                elevationColor = Color.White.copy(alpha = 0.1f),
                elevation = 8.dp
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // BASE PLACEHOLDER
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
                ShimmerBox()
            }

            AsyncImage(
                model = imageRequest,
                imageLoader = PeakImageLoader.getInstance(context),
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Skeleton placeholder for the Home Screen Movie Rows.
 * Normalised to 220dp to match the StableMovieCardWrapper footprint.
 */
@Composable
fun SkeletonMovieCard() {
    Surface(
        onClick = { /* Do nothing while loading */ },
        modifier = Modifier
            .width(145.dp)
            .height(220.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF2A2A2A),
            focusedContainerColor = Color(0xFF3A3A3A)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        ShimmerBox()
    }
}
