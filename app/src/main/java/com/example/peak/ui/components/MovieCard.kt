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
            .width(140.dp)
            .aspectRatio(2f / 3f)
            .onFocusChanged {
                if (it.isFocused) {
                    onMovieFocused(movie)
                }
            },
        scale = CardDefaults.scale(focusedScale = 1.1f),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
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
