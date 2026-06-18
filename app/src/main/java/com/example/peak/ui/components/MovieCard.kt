package com.example.peak.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.zIndex
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.peak.domain.model.Movie
import com.example.peak.ui.image.PeakImageLoader

/**
 * Large, cinematic Movie Card for the Home Screen.
 * Optimized for performance: asynchronous loading with stable placeholders.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieCard(
    movie: Movie,
    isSettled: Boolean, // Synced with Row-level timing
    onFocus: (Movie?) -> Unit,
    onClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    
    // Switch to high-res backdrop only when the Row tells us it's settled
    val displayImageUrl = if (isSettled) movie.backdropUrl else movie.imageUrl
    
    val imageRequest = remember(displayImageUrl) {
        PeakImageLoader.buildRequest(context, displayImageUrl)
    }

    Card(
        onClick = { onClick(movie) },
        modifier = modifier
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) {
                    onFocus(movie)
                }
            },
        scale = CardDefaults.scale(focusedScale = 1.1f),
        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.8f)),
                inset = 0.dp
            )
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(
                elevationColor = Color.White.copy(alpha = 0.15f),
                elevation = 12.dp
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. BASE PLACEHOLDER (Always present, prevents grey tiles)
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
                ShimmerBox()
            }

            // 2. ASYNC IMAGE (Loaded smoothly in background)
            AsyncImage(
                model = imageRequest,
                imageLoader = PeakImageLoader.getInstance(context),
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Progress Bar Overlay
            if (progress != null && progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
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
    val imageRequest = remember(movie.imageUrl) {
        PeakImageLoader.buildRequest(context, movie.imageUrl)
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
