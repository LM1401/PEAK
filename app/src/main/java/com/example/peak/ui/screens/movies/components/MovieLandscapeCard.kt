package com.example.peak.ui.screens.movies.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.peak.domain.model.Movie
import com.example.peak.ui.image.PeakImageLoader
import com.example.peak.ui.screens.movies.MoviesConstants

/**
 * Compact 16:9 Landscape Movie Card for Movies Screen (Phase 4 Target Design).
 * Optimized for TV D-pad focus: GPU-accelerated 1.05x scale, vibrant cyan border glow,
 * and zero layout reflow.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieLandscapeCard(
    movie: Movie,
    onFocus: (Movie) -> Unit,
    onClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val context = LocalContext.current
    val imageLoader = remember { PeakImageLoader.getInstance(context) }
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        label = "MovieLandscapeCardScale"
    )

    val shape = RoundedCornerShape(MoviesConstants.LANDSCAPE_CARD_CORNER_RADIUS)

    Card(
        onClick = { onClick(movie) },
        modifier = modifier
            .width(MoviesConstants.LANDSCAPE_CARD_WIDTH)
            .height(MoviesConstants.LANDSCAPE_CARD_HEIGHT)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) {
                    onFocus(movie)
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin.Center
                clip = false
            },
        scale = CardDefaults.scale(focusedScale = 1.0f),
        shape = CardDefaults.shape(shape = shape),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color(0xFF00D2FF)),
                inset = 0.dp
            ),
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                inset = 0.dp
            )
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(
                elevationColor = Color(0xFF00D2FF).copy(alpha = 0.45f),
                elevation = 16.dp
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF141E30))
                .clip(shape)
        ) {
            // 1. LANDSCAPE BACKDROP ARTWORK
            val backdropData = if (movie.backdropUrl.isNotBlank()) movie.backdropUrl else movie.imageUrl
            val cacheKey = "${movie.mediaType.name}_${movie.movieId}_landscape"

            AsyncImage(
                model = remember(movie.movieId, backdropData) {
                    ImageRequest.Builder(context)
                        .data(backdropData)
                        .memoryCacheKey(cacheKey)
                        .diskCacheKey(cacheKey)
                        .crossfade(true)
                        .allowHardware(true)
                        .build()
                },
                imageLoader = imageLoader,
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 2. BOTTOM GRADIENT OVERLAY
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 40f
                        )
                    )
            )

            // 3. TITLE / TITLE LOGO OVERLAY
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                if (!movie.titleLogoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = remember(movie.movieId, movie.titleLogoUrl) {
                            ImageRequest.Builder(context)
                                .data(movie.titleLogoUrl)
                                .crossfade(true)
                                .build()
                        },
                        contentDescription = movie.name,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomStart,
                        modifier = Modifier
                            .height(28.dp)
                            .fillMaxWidth(0.85f)
                    )
                } else {
                    Text(
                        text = movie.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
