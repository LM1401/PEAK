package com.example.peak.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.tv.material3.Surface
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.ui.focus.FocusMemoryManager
import com.example.peak.ui.image.ImageWarmingManager
import com.example.peak.ui.image.PeakImageLoader
import com.example.peak.ui.screens.home.HomeConstants
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

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
    focusManager: FocusMemoryManager? = null,
    restorationMovieId: String? = null,
    restorationMediaType: MediaType? = null,
    onRestorationComplete: () -> Unit = {},
    isFocused: Boolean = false,
    isNearViewport: Boolean = true,
    onVerticalMove: ((String, Int) -> Unit)? = null,
    prevRowId: String? = null,
    prevRowSize: Int = 0,
    nextRowId: String? = null,
    nextRowSize: Int = 0,
    focusRegistry: com.example.peak.ui.focus.MovieRowFocusManager? = null,
    navFocusRequester: FocusRequester? = null
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val imageLoader = remember { PeakImageLoader.getInstance(context) }
    
    // BACKWARD COMPATIBILITY: Internal map if no registry provided
    val localFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }

    fun getRequester(index: Int, movie: Movie): FocusRequester {
        return focusRegistry?.getCardRequester(row.id, index)
            ?: localFocusRequesters.getOrPut("${movie.mediaType.name}_${movie.movieId}") { FocusRequester() }
    }

    // PRECISION RESTORATION: Explicitly targets the card FocusRequester after materialization
    LaunchedEffect(restorationMovieId, restorationMediaType) {
        if (restorationMovieId != null && restorationMediaType != null) {
            val index = row.movies.indexOfFirst {
                it.movieId == restorationMovieId && it.mediaType == restorationMediaType
            }
            if (index != -1) {
                // 1. Force materialization
                listState.scrollToItem(index)

                // 2. Wait for composition to stabilize
                val requester = getRequester(index, row.movies[index])
                // We can't easily check 'contains' on the registry, but we can wait for a frame
                kotlinx.coroutines.delay(16)

                // 3. Precision Focus
                requester.requestFocus()

                // 4. Signal completion to clear restoration state
                onRestorationComplete()
            }
        }
    }

    // PREDICTIVE SCROLL PRE-DECODING (WARMING)
    // Optimized: Keyed by row.id to prevent restarts on progressive data arrivals.
    // Window reduced to 5 for lighter speculative workload.
    LaunchedEffect(isNearViewport, row.id) {
        if (!isNearViewport) return@LaunchedEffect

        snapshotFlow { Pair(listState.firstVisibleItemIndex, row.movies) }
            .distinctUntilChanged()
            .collectLatest { (index, movies) ->
                if (movies.isNotEmpty()) {
                    val windowSize = 5
                    val endIndex = (index + windowSize).coerceAtMost(movies.size)
                    val moviesToWarm = movies.subList(index, endIndex)
                    // ROW SCROLL: Warm posters only, speculative
                    ImageWarmingManager.warm(context, imageLoader, moviesToWarm, warmBackdrops = false)
                }
            }
    }

    var isRowFocusedInternal by remember { mutableStateOf(false) }

    // Persist focus and trigger DIRECT WARMING on focus change
    LaunchedEffect(isFocused, focusedMovieId) {
        if (!isFocused) return@LaunchedEffect

        val index = row.movies.indexOfFirst { it.movieId == focusedMovieId }
        val focused = row.movies.getOrNull(index)

        focused?.let {
            focusManager?.saveFocus(row.id, it.movieId)

            // DIRECT WARMING: Fire-and-forget immediate decode trigger
            // For focused items, we warm the BACKDROP as well.
            ImageWarmingManager.warm(context, imageLoader, listOf(it), warmBackdrops = true)

            // Predictive pre-decoding for neighbors - Posters only
            val nextMovie = row.movies.getOrNull(index + 1)
            val prevMovie = row.movies.getOrNull(index - 1)
            ImageWarmingManager.warm(context, imageLoader, listOfNotNull(nextMovie, prevMovie), warmBackdrops = false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeConstants.HOME_ROW_SLOT_HEIGHT - HomeConstants.HOME_ROW_SPACING)
            .padding(top = if (isFocused) 54.dp else 4.dp)
            .onFocusChanged { focusState ->
                if (focusState.hasFocus && !isRowFocusedInternal) {
                    // ENTRY RESTORATION: When row gains focus from outside, restore last known position
                    val lastId = focusManager?.getRememberedId(row.id)
                    if (lastId != null) {
                        val index = row.movies.indexOfFirst { it.movieId == lastId }
                        if (index != -1) {
                            getRequester(index, row.movies[index]).requestFocus()
                        }
                    }
                }
                isRowFocusedInternal = focusState.hasFocus
            },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = row.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = HomeConstants.HOME_CONTENT_START_PADDING)
        )

        // SINGLE LAZYROW: Prevents structural disposal during placeholder -> content transition.
        LazyRow(
            state = listState,
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth(),
            contentPadding = PaddingValues(
            start = HomeConstants.HOME_CONTENT_START_PADDING,
            end = 120.dp,
            top = 0.dp,
            bottom = 4.dp
        ),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (row.isPlaceholder) {
                // SKELETON SLOT: Stable keys to maintain focus during loading state
                items(10, key = { index -> "${row.id}_skeleton_slot_$index" }) {
                    SkeletonMovieCard()
                }
            } else {
                // CONTENT SLOT: Stable Data ID keys to ensure focus survives recomposition and metadata updates
                itemsIndexed(
                    items = row.movies,
                    key = { _, movie -> "${row.id}_${movie.mediaType.name}_${movie.movieId}" }
                ) { index, movie ->
                    val focusRequester = getRequester(index, movie)

                    val prevFocusRequester = if (index > 0) getRequester(index - 1, row.movies[index - 1]) else null

                    StableMovieCardWrapper(
                        movie = movie,
                        index = index,
                        progress = progressMap?.get("${movie.mediaType.name}_${movie.movieId}"),
                        focusRequester = focusRequester,
                        prevFocusRequester = prevFocusRequester,
                        onFocus = { m -> m?.let { onMovieFocused(row.id, it.movieId, it.mediaType) } },
                        onMovieSelected = onMovieSelected,
                        onMovieClick = onMovieClick,
                        onVerticalMove = onVerticalMove,
                        prevRowId = prevRowId,
                        prevRowSize = prevRowSize,
                        nextRowId = nextRowId,
                        nextRowSize = nextRowSize,
                        navFocusRequester = navFocusRequester
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
    index: Int,
    progress: Float?,
    focusRequester: FocusRequester,
    prevFocusRequester: FocusRequester? = null,
    onFocus: (Movie?) -> Unit,
    onMovieSelected: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onVerticalMove: ((String, Int) -> Unit)? = null,
    prevRowId: String? = null,
    prevRowSize: Int = 0,
    nextRowId: String? = null,
    nextRowSize: Int = 0,
    navFocusRequester: FocusRequester? = null
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
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown) {
                    when (it.key) {
                        Key.DirectionDown -> {
                            if (nextRowId != null && onVerticalMove != null) {
                                onVerticalMove(nextRowId, index.coerceAtMost(nextRowSize - 1))
                                true
                            } else if (nextRowId == null) {
                                true // BOUNDARY PROTECTION: Cancel move
                            } else false
                        }
                        Key.DirectionUp -> {
                            if (prevRowId != null && onVerticalMove != null) {
                                onVerticalMove(prevRowId, index.coerceAtMost(prevRowSize - 1))
                                true
                            } else if (prevRowId == null) {
                                true // BOUNDARY PROTECTION: Cancel move
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
            .zIndex(if (isLocalFocused) 10f else 1f)
    ) {
        MovieCard(
            movie = movie,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .focusRequester(focusRequester)
                .focusProperties {
                    // HORIZONTAL DETERMINISM
                    left = if (index == 0) {
                        navFocusRequester ?: left
                    } else {
                        prevFocusRequester ?: left
                    }
                },
            progress = progress,
            onFocus = onFocus,
            onClick = {
                onMovieSelected(movie)
                onMovieClick(movie)
            }
        )
    }
}
