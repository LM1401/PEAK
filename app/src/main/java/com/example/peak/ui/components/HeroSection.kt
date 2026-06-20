package com.example.peak.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.peak.domain.model.Movie

/**
 * Compact Netflix TV 2024-style Hero Header.
 * Optimized for frame-synced metadata updates with zero stale frame retention.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HeroSection(
    movie: Movie?,
    modifier: Modifier = Modifier
) {
    key(movie?.movieId) {
        Box(
            modifier = modifier
                .background(Color.Transparent)
                .heightIn(min = 100.dp),
            contentAlignment = Alignment.TopStart
        ) {
            AnimatedContent(
                targetState = movie,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(120, easing = FastOutSlowInEasing)
                    ) togetherWith fadeOut(
                        animationSpec = tween(120, easing = FastOutSlowInEasing)
                    )
                },
                label = "HeroTransition"
            ) { currentMovie ->
                if (currentMovie != null) {
                    Text(
                        text = currentMovie.name,
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }
        }
    }
}
