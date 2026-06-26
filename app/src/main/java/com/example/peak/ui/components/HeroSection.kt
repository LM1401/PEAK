package com.example.peak.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.peak.domain.model.Movie

/**
 * Cinematic Hero Section HUD.
 * Refined for visual hierarchy: title is supporting context, not dominant.
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
                .fillMaxWidth()
                .wrapContentHeight(), // Dynamic height to allow tight spacing with rows
            contentAlignment = Alignment.TopStart
        ) {
            AnimatedContent(
                targetState = movie,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(300, delayMillis = 60, easing = FastOutSlowInEasing)
                    ) togetherWith fadeOut(
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    )
                },
                label = "HeroTransition"
            ) { currentMovie ->
                if (currentMovie != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // 1. TITLE (headlineLarge for dominance)
                        Text(
                            text = currentMovie.name,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // 2. METADATA ROW
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = currentMovie.duration,
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = currentMovie.year,
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            val rating = currentMovie.rating
                            if (rating.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = rating,
                                        color = Color.White.copy(alpha = 0.9f),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFF5C518), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "IMDb",
                                            color = Color.Black,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                            }
                            
                            HeroBadge(text = currentMovie.ageRating)

                            Text(
                                text = currentMovie.genres.replace(", ", " | "),
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelLarge
                            )

                            // Technical Badges (Ownership moved from MovieCard)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TechnicalBadge(text = "4K")
                                TechnicalBadge(text = "HDR")
                                TechnicalBadge(text = "Dolby Atmos")
                            }
                        }

                        // 3. DESCRIPTION (Max 4 lines for depth)
                        Text(
                            text = currentMovie.description,
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 22.sp,
                            modifier = Modifier.widthIn(max = 800.dp)
                        )

                        // 4. SECONDARY METADATA (Director & Cast)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (currentMovie.director.isNotBlank()) {
                                Text(
                                    text = "Directed by ${currentMovie.director}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            if (currentMovie.cast.isNotBlank()) {
                                Text(
                                    text = "Cast: ${currentMovie.cast}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 5. BUTTONS (Restored for Action Zone)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HeroButton(icon = Icons.Default.PlayArrow, text = "Play")
                            HeroButton(icon = Icons.Default.Add, text = "Watchlist")
                            HeroButton(icon = Icons.Default.Info, text = "Details")
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun HeroBadge(text: String) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TechnicalBadge(text: String) {
    Box(
        modifier = Modifier
            .background(Color.Transparent, RoundedCornerShape(2.dp))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroButton(icon: ImageVector, text: String) {
    Surface(
        onClick = { /* Actions handled at screen level */ },
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.1f),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}
