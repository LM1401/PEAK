package com.example.peak.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.peak.domain.model.Movie

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HeroSection(
    movie: Movie?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black), // Deep black background
        contentAlignment = Alignment.BottomStart
    ) {
        AnimatedContent(
            targetState = movie,
            transitionSpec = {
                fadeIn() with fadeOut()
            },
            label = "HeroTransition"
        ) { currentMovie ->
            if (currentMovie != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val backdrop = currentMovie.backdropUrl
                    val imageUrl = remember(backdrop) {
                        when {
                            backdrop.startsWith("/") -> "https://image.tmdb.org/t/p/w1280$backdrop"
                            backdrop.isNotBlank() -> backdrop
                            else -> null
                        }
                    }

                    if (imageUrl != null) {
                        val context = LocalContext.current
                        val imageRequest = remember(imageUrl) {
                            ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build()
                        }
                        Image(
                            painter = rememberAsyncImagePainter(model = imageRequest),
                            contentDescription = currentMovie.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = 1.1f
                                    scaleY = 1.1f
                                },
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Cinematic Gradient Overlay (Top transparent -> Bottom solid dark)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Black.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )
                    
                    // Side fade for depth (Left dark -> Right transparent)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.8f),
                                        Color.Transparent
                                    ),
                                    startX = 0f,
                                    endX = 1000f
                                )
                            )
                    )

                    // Info Content
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 48.dp, bottom = 48.dp)
                            .fillMaxWidth(0.65f)
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

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = currentMovie.description,
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 24.sp
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Select a movie", color = Color.Gray)
                }
            }
        }
    }
}
