package com.example.peak.ui.screens.movies.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.peak.domain.model.Movie
import com.example.peak.ui.screens.movies.MoviesConstants

/**
 * Reusable metadata component for the currently focused movie.
 * Designed to have a fixed height to prevent layout jitter during horizontal navigation.
 */
@Composable
fun FocusedMovieMetadata(
    movie: Movie?,
    modifier: Modifier = Modifier
) {
    // Fixed vertical footprint to prevent layout bouncing during horizontal scrolling
    Box(
        modifier = modifier
            .height(132.dp)
            .fillMaxWidth()
            .padding(
                start = MoviesConstants.CONTENT_START_PADDING,
                end = MoviesConstants.CONTENT_END_PADDING,
                top = 16.dp,
                bottom = 12.dp
            )
    ) {
        if (movie != null) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Title
                Text(
                    text = movie.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Specs Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val specs = listOfNotNull(
                        movie.year.takeIf { it.isNotBlank() },
                        movie.duration.takeIf { it.isNotBlank() },
                        movie.ageRating.takeIf { it.isNotBlank() },
                        movie.rating.takeIf { it.isNotBlank() }?.let { "$it/10" }
                    ).joinToString(" • ")
                    
                    Text(
                        text = specs,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Genres
                if (movie.genres.isNotBlank()) {
                    Text(
                        text = movie.genres,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Description
                if (movie.description.isNotBlank()) {
                    Text(
                        text = movie.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
