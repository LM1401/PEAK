package com.example.peak.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import coil.request.ImageRequest
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.ui.focus.FocusMemoryManager
import com.example.peak.ui.image.ImageWarmingManager
import com.example.peak.ui.image.PeakImageLoader
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Reusable Movie Row component for TV browsing screens.
 * Optimized for GPU-accelerated expansion and synchronous focus warming.
 */
@Composable
fun MovieRow(
    row: Row,
    focusedMovieId: String?,
    onMovieFocused: (String) -> Unit,
    onMovieSelected: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    progressMap: Map<String, Float>? = null,
    focusManager: FocusMemoryManager? = null
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val imageLoader = remember { PeakImageLoader.getInstance(context) }

    // PREDICTIVE SCROLL PRE-DECODING (WARMING)
    LaunchedEffect(row.movies) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { index ->
                if (row.movies.isNotEmpty()) {
                    val endIndex = (index + 10).coerceAtMost(row.movies.size)
                    val moviesToWarm = row.movies.subList(index, endIndex)
                    ImageWarmingManager.warm(context, imageLoader, moviesToWarm)
                }
            }
    }

    var isRowFocused by remember { mutableStateOf(false) }

    // Persist focus and trigger DIRECT WARMING on focus change
    LaunchedEffect(focusedMovieId) {
        val index = row.movies.indexOfFirst { it.movieId == focusedMovieId }
        val focused = row.movies.getOrNull(index)

        focused?.let {
            focusManager?.saveFocus(row.id, it.movieId)
            
            // DIRECT WARMING: Fire-and-forget immediate decode trigger
            ImageWarmingManager.warm(context, imageLoader, listOf(it))

            // Predictive pre-decoding for neighbors
            val nextMovie = row.movies.getOrNull(index + 1)
            val prevMovie = row.movies.getOrNull(index - 1)
            ImageWarmingManager.warm(context, imageLoader, listOfNotNull(nextMovie, prevMovie))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .onFocusChanged { focusState ->
                isRowFocused = focusState.hasFocus
            }
    ) {
        Text(
            text = row.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 120.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (row.isPlaceholder) {
            LazyRow(
                modifier = Modifier
                    .height(320.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(10) {
                    SkeletonMovieCard()
                }
            }
        } else {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .height(320.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(
                    items = row.movies,
                    key = { it.movieId }
                ) { movie ->
                    val isFocused = focusedMovieId == movie.movieId
                    val focusRequester = remember(movie.movieId) { FocusRequester() }

                    // Restore focus if this was the last focused item in this row
                    LaunchedEffect(isRowFocused) {
                        if (isRowFocused) {
                            val lastId = focusManager?.getRememberedId(row.id)
                            if (lastId == movie.movieId) {
                                focusRequester.requestFocus()
                            }
                        }
                    }

                    StableMovieCardWrapper(
                        movie = movie,
                        isFocused = isFocused,
                        progress = progressMap?.get(movie.movieId),
                        focusRequester = focusRequester,
                        onFocus = { m -> m?.let { onMovieFocused(it.movieId) } },
                        onMovieSelected = onMovieSelected,
                        onMovieClick = onMovieClick
                    )
                }
            }
        }
    }
}

/**
 * Performance-optimised wrapper.
 * Uses GPU scale transformations instead of width-based layout remeasurement.
 */
@Composable
private fun StableMovieCardWrapper(
    movie: Movie,
    isFocused: Boolean,
    progress: Float?,
    focusRequester: FocusRequester,
    onFocus: (Movie?) -> Unit,
    onMovieSelected: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    // GPU-ACCELERATED SCALE (Replaces width animation to prevent jitter)
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1.0f, // Adjusted for safe TV overscan
        animationSpec = tween(
            durationMillis = 220, // Perfectly balanced for TV focus response
            easing = FastOutSlowInEasing
        ),
        label = "GPUExpansion"
    )

    MovieCard(
        movie = movie,
        isFocused = isFocused,
        modifier = Modifier
            .width(145.dp) // Reduced width to match reference
            .height(215.dp) // Reduced height to match reference
            .zIndex(if (isFocused) 10f else 1f) // CRITICAL: Ensure expanded card stays on top of neighbors
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // Apply a slight shadow elevation on expansion for depth
                shadowElevation = if (isFocused) 12f else 0f
                shape = RoundedCornerShape(8.dp)
                clip = false
            }
            .focusRequester(focusRequester),
        progress = progress,
        onFocus = onFocus,
        onClick = { 
            onMovieSelected(movie)
            onMovieClick(movie)
        }
    )
}
