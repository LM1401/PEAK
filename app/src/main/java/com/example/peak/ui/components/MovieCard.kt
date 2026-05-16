package com.example.peak.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.peak.domain.model.Movie

/**
 * Large, cinematic Movie Card for the Home Screen.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieCard(
    movie: Movie,
    onFocus: (Movie?) -> Unit,
    onClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    
    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.75f,
        label = "cardAlpha"
    )

    val imageRequest = remember(movie.imageUrl) {
        ImageRequest.Builder(context)
            .data(movie.imageUrl)
            .crossfade(true)
            .build()
    }

    Card(
        onClick = { onClick(movie) },
        modifier = modifier
            .width(200.dp)
            .aspectRatio(2f / 3f)
            .padding(8.dp)
            .alpha(alpha)
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) {
                    onFocus(movie)
                }
            },
        scale = CardDefaults.scale(focusedScale = 1.15f),
        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
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
            Image(
                painter = rememberAsyncImagePainter(model = imageRequest),
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
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
        ImageRequest.Builder(context)
            .data(movie.imageUrl)
            .crossfade(true)
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
            Image(
                painter = rememberAsyncImagePainter(model = imageRequest),
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
