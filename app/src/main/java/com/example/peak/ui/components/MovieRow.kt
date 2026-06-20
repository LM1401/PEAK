package com.example.peak.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import coil.request.ImageRequest
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.ui.focus.FocusMemoryManager
import com.example.peak.ui.image.ImagePreloader
import com.example.peak.ui.image.ImageWarmingManager
import com.example.peak.ui.image.PeakImageLoader
import coil.ImageLoader
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Reusable Movie Row component for TV browsing screens.
 * Refactored to use direct state snapshot for focus to ensure stability and predictability.
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
                    val urls = row.movies.subList(index, endIndex)
                        .flatMap { listOf(it.imageUrl, it.backdropUrl) }
                    ImageWarmingManager.warm(context, imageLoader, urls)
                }
            }
    }

    var isRowFocused by remember { mutableStateOf(false) }

    // Restore focus when the row is re-entered
    LaunchedEffect(isRowFocused) {
        if (isRowFocused) {
            // Restore logic is now handled per-item in the LazyRow loop to prevent stale FocusRequesters
        }
    }

    // Persist focus to manager and PRELOAD BOTH IMAGE TYPES
    LaunchedEffect(focusedMovieId) {
        val index = row.movies.indexOfFirst { it.movieId == focusedMovieId }
        val focused = row.movies.getOrNull(index)

        focused?.let {
            focusManager?.saveFocus(row.id, it.movieId)
            
            // PRELOAD POSTER
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(it.imageUrl)
                    .crossfade(false)
                    .build()
            )

            // PRELOAD BACKDROP
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(it.backdropUrl)
                    .crossfade(false)
                    .build()
            )

            // PREDICTIVE PRE-DECODING (WARMING): Removes decode flash during D-pad navigation
            val nextMovie = row.movies.getOrNull(index + 1)
            val prevMovie = row.movies.getOrNull(index - 1)
            
            ImageWarmingManager.warm(
                context = context,
                imageLoader = imageLoader,
                urls = listOfNotNull(
                    nextMovie?.backdropUrl,
                    prevMovie?.backdropUrl
                )
            )
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
            modifier = Modifier.padding(start = 120.dp, bottom = 12.dp)
        )

        if (row.isPlaceholder) {
            LazyRow(
                modifier = Modifier
                    .height(340.dp)
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
                    .height(340.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(
                    items = row.movies,
                    key = { it.movieId }
                ) { movie ->
                    // Direct focus signal from ViewModel to restore continuous expansion identity
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
 * Performance-optimised wrapper to isolate card recomposition and expansion animations.
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
    val animatedWidth by animateDpAsState(
        targetValue = if (isFocused) 430.dp else 180.dp,
        animationSpec = tween(
            durationMillis = 280,
            easing = FastOutSlowInEasing
        ),
        label = "NetflixCardWidth"
    )

    MovieCard(
        movie = movie,
        isFocused = isFocused,
        modifier = Modifier
            .width(animatedWidth)
            .height(270.dp)
            .focusRequester(focusRequester),
        progress = progress,
        onFocus = onFocus,
        onClick = { 
            onMovieSelected(movie)
            onMovieClick(movie)
        }
    )
}
