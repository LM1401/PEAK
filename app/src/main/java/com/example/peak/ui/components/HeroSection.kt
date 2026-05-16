package com.example.peak.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.peak.domain.model.Movie

/**
 * Compact Netflix TV 2024-style Hero Header.
 * Displays title, metadata, and description for the currently selected movie.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HeroSection(
    movie: Movie?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Transparent),
        contentAlignment = Alignment.TopStart
    ) {
        AnimatedContent(
            targetState = movie,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                ) with fadeOut(
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            },
            label = "HeroTransition"
        ) { currentMovie ->
            if (currentMovie != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 120.dp, end = 48.dp), // NETFLIX 2024 GRID: 120dp safe margin
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = currentMovie.name,
                        color = Color.White,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            shadow = Shadow(
                                color = Color.Black,
                                offset = Offset(2f, 4f),
                                blurRadius = 8f
                            )
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metadata Row: Year | Duration | Rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = currentMovie.year,
                            color = Color.White.copy(alpha = 0.7f),
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
                                text = currentMovie.ageRating,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = currentMovie.duration,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentMovie.description,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 3, // Floating overlay needs a bit more room
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(0.dp))
            }
        }
    }
}
