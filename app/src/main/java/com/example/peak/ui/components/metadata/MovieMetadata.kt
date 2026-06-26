package com.example.peak.ui.components.metadata

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.peak.domain.model.Movie

/**
 * Supporting Detail Layer HUD.
 * Refined to show technical details and secondary metadata without competing with Hero.
 */
@Composable
fun MovieMetadata(
    movie: Movie,
    modifier: Modifier = Modifier
) {
    // Technical metadata ownership moved to HeroSection.
    // This component remains as a structural placeholder for global HUD layering.
    Spacer(modifier = modifier.height(1.dp))
}

/**
 * MovieMetadataSection is a wrapper that ensures the metadata is displayed
 * as a consistent floating HUD.
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
            .height(120.dp) // Optimized height for gradient + badges
            .padding(bottom = 32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedContent(
            targetState = movie,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(400, delayMillis = 120) // Ripple staggered entry
                ) togetherWith fadeOut(
                    animationSpec = tween(200)
                )
            },
            label = "MetadataTransition"
        ) { currentMovie ->
            if (currentMovie != null) {
                MovieMetadata(
                    movie = currentMovie,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
