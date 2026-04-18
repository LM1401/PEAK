package com.example.peak.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.rememberAsyncImagePainter
import com.example.peak.domain.model.Movie

/**
 * Reusable Movie Card component for TV.
 * Handles focus scaling and glow effects.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieCard(
    movie: Movie, 
    onMovieFocused: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    Card(
        onClick = { onMovieClick(movie) },
        modifier = Modifier
            .width(150.dp) // slightly wider for better TV presence
            .aspectRatio(2f / 3f)
            .padding(8.dp) // padding to prevent clipping during focus scale
            .onFocusChanged {
                if (it.isFocused) {
                    onMovieFocused(movie)
                }
            },
        scale = CardDefaults.scale(focusedScale = 1.12f),
        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
        glow = CardDefaults.glow(
            focusedGlow = Glow(
                elevationColor = Color.White.copy(alpha = 0.1f),
                elevation = 10.dp
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(movie.imageUrl),
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
