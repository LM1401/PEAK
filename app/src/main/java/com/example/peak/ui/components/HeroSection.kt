package com.example.peak.ui.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
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
    focusedMovieProvider: @Composable () -> Movie?,
    modifier: Modifier = Modifier
) {
    val movie = focusedMovieProvider()
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
                        // 1. PRODUCTION COMPANY BADGE
                        Log.d("PEAK_DIAGNOSTIC", "HeroSection: Rendering '${currentMovie.name}', studio: '${currentMovie.productionCompany}'")
                        if (currentMovie.productionCompany.isNotBlank()) {
                            Log.d("PEAK_DIAGNOSTIC", "HeroSection: Drawing badge for '${currentMovie.productionCompany}'")
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
                                    text = currentMovie.productionCompany.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

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

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. METADATA ROW
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = currentMovie.year,
                                color = Color.White.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.labelMedium,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "•",
                                color = Color.White.copy(alpha = 0.2f)
                            )
                            Text(
                                text = currentMovie.genres.split(",").firstOrNull() ?: "",
                                color = Color.White.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.labelMedium,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "•",
                                color = Color.White.copy(alpha = 0.2f)
                            )
                            Text(
                                text = currentMovie.duration,
                                color = Color.White.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.labelMedium,
                                letterSpacing = 1.2.sp
                            )
                        }

                        // 4. CAST ROW
                        if (currentMovie.cast.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "CAST:",
                                    color = Color.White.copy(alpha = 0.45f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = currentMovie.cast.replace(", ", "  ·  "),
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 5. DESCRIPTION
                        Text(
                            text = currentMovie.description,
                            color = Color.White.copy(alpha = 0.60f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 24.sp,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.fillMaxWidth().height(200.dp))
                }
            }
        }
    }
}


