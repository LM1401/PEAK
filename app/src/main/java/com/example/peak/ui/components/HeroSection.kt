package com.example.peak.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.peak.domain.model.Movie
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow

private val HERO_CONTENT_WIDTH = 640.dp

/**
 * Cinematic Hero Section HUD.
 * Redesigned to match the Premium Streaming Concept.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun HeroSection(
    movie: Movie?,
    modifier: Modifier = Modifier
) {
    key(movie?.movieId) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.TopStart
        ) {
            AnimatedContent(
                targetState = movie,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    ) togetherWith fadeOut(
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                },
                label = "HeroTransition"
            ) { currentMovie ->
                if (currentMovie != null) {
                    Column(
                        modifier = Modifier.width(HERO_CONTENT_WIDTH),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // 1. PEAK ORIGINAL BADGE
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "PEAK ORIGINAL",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. TITLE (Cinematic typography)
                        Text(
                            text = currentMovie.name.uppercase(),
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 52.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. METADATA ROW
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = currentMovie.year,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "•",
                                color = Color.White.copy(alpha = 0.4f)
                            )
                            Text(
                                text = currentMovie.genres.split(",").firstOrNull() ?: "",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "•",
                                color = Color.White.copy(alpha = 0.4f)
                            )
                            Text(
                                text = currentMovie.duration,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelLarge
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TechnicalBadge(text = "HD")
                                TechnicalBadge(text = "4K")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Dolby Atmos",
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. DESCRIPTION
                        Text(
                            text = currentMovie.description,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.fillMaxWidth().height(200.dp))
                }
            }
        }
    }
}

@Composable
private fun TechnicalBadge(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp
        )
    }
}
