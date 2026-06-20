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
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.ui.focus.FocusMemoryManager
import com.example.peak.ui.image.ImagePreloader
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
    onMovieFocused: (Movie?) -> Unit,
    onMovieSelected: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    progressMap: Map<String, Float>? = null,
    focusManager: FocusMemoryManager? = null
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // PREDICTIVE SCROLL PRELOADING
    LaunchedEffect(row.movies) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { index ->
                if (row.movies.isNotEmpty()) {
                    val endIndex = (index + 10).coerceAtMost(row.movies.size)
                    val urls = row.movies.subList(index, endIndex)
                        .flatMap { listOf(it.imageUrl, it.backdropUrl) }
                    ImagePreloader.preload(context, urls)
                }
            }
    }

    var isRowFocused by remember { mutableStateOf(false) }
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    // Restore focus when the row is re-entered
    LaunchedEffect(isRowFocused) {
        if (isRowFocused) {
            val lastId = focusManager?.getRememberedId(row.id)
            if (lastId != null) {
                focusRequesters[lastId]?.requestFocus()
            }
        }
    }

    // Persist focus to manager
    LaunchedEffect(focusedMovieId) {
        val matchingMovie = row.movies.find { it.movieId == focusedMovieId }
        if (matchingMovie != null) {
            focusManager?.saveFocus(row.id, matchingMovie.movieId)
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

                StableMovieCardWrapper(
                    movie = movie,
                    isFocused = isFocused,
                    progress = progressMap?.get(movie.movieId),
                    focusRequester = focusRequesters.getOrPut(movie.movieId) {
                        FocusRequester()
                    },
                    onFocus = onMovieFocused,
                    onMovieSelected = onMovieSelected,
                    onMovieClick = onMovieClick
                )
            }
        }

        // ISOLATED METADATA SECTION
        MovieMetadataSection(
            movies = row.movies,
            focusedMovieId = focusedMovieId,
            isRowFocused = isRowFocused
        )
    }
}

/**
 * Isolated metadata section to prevent entire MovieRow from recomposing.
 */
@Composable
private fun MovieMetadataSection(
    movies: List<Movie>,
    focusedMovieId: String?,
    isRowFocused: Boolean
) {
    val metaTransitionDuration = 350
    
    // Visibility logic: Row must have focus AND contain the focused movie
    val focusedMovieInRow = if (isRowFocused) {
        movies.find { it.movieId == focusedMovieId }
    } else null

    androidx.compose.animation.AnimatedVisibility(
        visible = focusedMovieInRow != null,
        enter = androidx.compose.animation.expandVertically(
            animationSpec = tween(metaTransitionDuration, easing = FastOutSlowInEasing)
        ) + androidx.compose.animation.fadeIn(animationSpec = tween(metaTransitionDuration)),
        exit = androidx.compose.animation.shrinkVertically(
            animationSpec = tween(metaTransitionDuration, easing = FastOutSlowInEasing)
        ) + androidx.compose.animation.fadeOut(animationSpec = tween(metaTransitionDuration))
    ) {
        focusedMovieInRow?.let { movie ->
            HeroSection(
                movie = movie,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
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
