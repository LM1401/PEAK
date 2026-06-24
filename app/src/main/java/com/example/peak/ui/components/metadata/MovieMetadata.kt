package com.example.peak.ui.components.metadata

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.peak.domain.model.Movie

/**
 * Dedicated Metadata component for the PEAK app.
 * Ensures consistent rendering of movie information across Hero and Row sections.
 */
@Composable
fun MovieMetadata(
    movie: Movie,
    modifier: Modifier = Modifier,
    isFocused: Boolean = true,
    showDescription: Boolean = true,
    progress: Float? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 120.dp), // NETFLIX 2024 GRID: 120dp safe margin
        verticalArrangement = Arrangement.Top
    ) {
        // 1. PRIMARY METADATA (Match Score, Year, Duration)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "98% Match",
                color = Color(0xFF46D369), // Netflix Green
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = movie.year,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelLarge
            )
            
            // Age Rating Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = movie.ageRating,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = movie.duration,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelLarge
            )
        }

        // 2. DESCRIPTION (Clean, readable under the row)
        if (showDescription) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = movie.description,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }

        // 3. SECONDARY METADATA (Genres/Cast)
        if (movie.genres.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = movie.genres,
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

/**
 * MovieMetadataSection is a wrapper that ensures the metadata is displayed
 * in a consistent way, especially when placed under rows.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MovieMetadataSection(
    movie: Movie?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
            .height(140.dp), // FIXED HEIGHT: Ensures LazyColumn contentPadding remains valid
        contentAlignment = Alignment.TopStart
    ) {
        AnimatedContent(
            targetState = movie,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(220) // Synced with Card expansion
                ) togetherWith fadeOut(
                    animationSpec = tween(220)
                )
            },
            label = "MetadataTransition"
        ) { currentMovie ->
            if (currentMovie != null) {
                MovieMetadata(
                    movie = currentMovie,
                    isFocused = true,
                    showDescription = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
