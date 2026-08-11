package com.example.peak.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.ClickableSurfaceDefaults
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
import com.example.peak.domain.model.MediaType
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
    onMovieFocused: (String, String, MediaType) -> Unit,
    onMovieSelected: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    progressMap: Map<String, Float>? = null,
    focusManager: FocusMemoryManager? = null
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val imageLoader = remember { PeakImageLoader.getInstance(context) }
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

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
            .onFocusChanged { focusState ->
                if (focusState.hasFocus && !isRowFocused) {
                    // ENTRY RESTORATION: When row gains focus from outside, restore last known position
                    val lastId = focusManager?.getRememberedId(row.id)
                    if (lastId != null) {
                        focusRequesters[lastId]?.requestFocus()
                    }
                }
                isRowFocused = focusState.hasFocus
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = row.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 120.dp)
        )

        // SINGLE LAZYROW: Prevents structural disposal during placeholder -> content transition.
        LazyRow(
            state = listState,
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 120.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (row.isPlaceholder) {
                // SKELETON SLOT: Stable keys to maintain focus during loading state
                items(10, key = { index -> "${row.id}_skeleton_slot_$index" }) {
                    SkeletonMovieCard()
                }
            } else {
                // CONTENT SLOT: Stable Data ID keys to ensure focus survives recomposition and metadata updates
                items(
                    items = row.movies,
                    key = { movie -> "${row.id}_${movie.mediaType.name}_${movie.movieId}" }
                ) { movie ->
                    val focusKey = "${movie.mediaType.name}_${movie.movieId}"
                    val focusRequester = remember(focusKey) { 
                        focusRequesters.getOrPut(focusKey) { FocusRequester() } 
                    }

                    StableMovieCardWrapper(
                        movie = movie,
                        progress = progressMap?.get("${movie.mediaType.name}_${movie.movieId}"),
                        focusRequester = focusRequester,
                        onFocus = { m -> m?.let { onMovieFocused(row.id, it.movieId, it.mediaType) } },
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
 * Refactored: Uses local focus state for frame-perfect expansion.
 */
@Composable
private fun StableMovieCardWrapper(
    movie: Movie,
    progress: Float?,
    focusRequester: FocusRequester,
    onFocus: (Movie?) -> Unit,
    onMovieSelected: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    // FIX: Local focus is the single source of truth for visual expansion
    var isLocalFocused by remember { mutableStateOf(false) }

    val cardWidth by animateDpAsState(
        targetValue = if (isLocalFocused) 320.dp else 145.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "card_width"
    )

    Column(
        modifier = Modifier
            .width(cardWidth)
            .padding(vertical = 10.dp)
            .onFocusChanged { 
                isLocalFocused = it.hasFocus 
            }
            .zIndex(if (isLocalFocused) 10f else 1f)
    ) {
        MovieCard(
            movie = movie,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .focusRequester(focusRequester),
            progress = progress,
            onFocus = onFocus,
            onClick = {
                onMovieSelected(movie)
                onMovieClick(movie)
            }
        )
    }
}
