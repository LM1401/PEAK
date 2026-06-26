package com.example.peak.ui.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
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
                .height(200.dp) // FIXED HEIGHT: Prevents layout shift during transition
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    Log.d("PEAK_HERO", "HeroSection:\nx=${pos.x}\ny=${pos.y}\nwidth=${coords.size.width}\nheight=${coords.size.height}")
                },
            contentAlignment = Alignment.BottomStart
        ) {
            AnimatedContent(
                targetState = movie,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(220, easing = FastOutSlowInEasing) // Synced with Card expansion
                    ) togetherWith fadeOut(
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
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
                    Spacer(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
