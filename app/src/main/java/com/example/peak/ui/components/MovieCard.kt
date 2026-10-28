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

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import com.example.peak.ui.components.sidebar.BookmarkIcon

/**
 * Large, cinematic Movie Card for the Home Screen.
 * Optimized for performance: asynchronous loading with stable placeholders and robust fallbacks.
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
            val posterKey = "${movie.mediaType.name}_${movie.movieId}"
            val backdropKey = "${posterKey}_backdrop"

            var isUsingBackdrop by remember { mutableStateOf(false) }
            LaunchedEffect(isLocalFocused, movie.backdropUrl) {
                if (isLocalFocused && movie.backdropUrl.isNotBlank()) {
                    isUsingBackdrop = false
                    kotlinx.coroutines.delay(250)
                    isUsingBackdrop = true
                } else {
                    isUsingBackdrop = false
                }
            }

            var isImageError by remember(movie.movieId, movie.mediaType, movie.imageUrl) { mutableStateOf(false) }

            if (movie.imageUrl.isBlank() || isImageError) {
                // Visual fallback for missing, invalid or failed image requests
                CardFallbackContent(movie = movie)
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
                    ShimmerBox()
                }

                val posterRequest = remember<ImageRequest>(
                    movie.movieId,
                    movie.mediaType,
                    movie.imageUrl
                ) {
                    ImageRequest.Builder(context)
                        .data(movie.imageUrl)
                        .memoryCacheKey(posterKey)
                        .diskCacheKey(posterKey)
                        .crossfade(200)
                        .allowHardware(true)
                        .build()
                }

                // Base Layer: Poster
                AsyncImage(
                    model = posterRequest,
                    imageLoader = imageLoader,
                    contentDescription = movie.name,
                    contentScale = ContentScale.Crop,
                    onError = { isImageError = true },
                    modifier = Modifier.fillMaxSize()
                )

                // Top Layer: Backdrop (only when focused)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isUsingBackdrop,
                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
                    exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val backdropRequest = remember<ImageRequest>(
                        movie.movieId,
                        movie.mediaType,
                        movie.backdropUrl
                    ) {
                        ImageRequest.Builder(context)
                            .data(movie.backdropUrl)
                            .memoryCacheKey(backdropKey)
                            .diskCacheKey(backdropKey)
                            .crossfade(200)
                            .allowHardware(true)
                            .build()
                    }
                    AsyncImage(
                        model = backdropRequest,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

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
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    focusedBorderColor: Color = Color.White
) {
    val context = LocalContext.current
    var isImageError by remember(movie.movieId, movie.mediaType, movie.imageUrl) { mutableStateOf(false) }

    val imageRequest = remember(movie.movieId, movie.mediaType, movie.imageUrl) {
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
        modifier = modifier
            .width(120.dp)
            .height(168.dp)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    onMovieFocused(movie)
                }
            },
        scale = CardDefaults.scale(focusedScale = 1.04f),
        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, focusedBorderColor),
                inset = 0.dp
            )
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(
                elevationColor = focusedBorderColor.copy(alpha = 0.3f),
                elevation = 8.dp
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (movie.imageUrl.isBlank() || isImageError) {
                CardFallbackContent(movie = movie)
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
                    ShimmerBox()
                }

                AsyncImage(
                    model = imageRequest,
                    imageLoader = PeakImageLoader.getInstance(context),
                    contentDescription = movie.name,
                    contentScale = ContentScale.Crop,
                    onError = { isImageError = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Clean PEAK visual fallback for cards missing poster artwork.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CardFallbackContent(movie: Movie) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = BookmarkIcon,
                contentDescription = null,
                tint = Color(0xFF00D2FF).copy(alpha = 0.8f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = movie.name,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (movie.year.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = movie.year,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
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
