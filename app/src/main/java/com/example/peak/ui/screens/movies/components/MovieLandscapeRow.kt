package com.example.peak.ui.screens.movies.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.peak.domain.model.Movie
import com.example.peak.ui.focus.FocusMemoryManager
import com.example.peak.ui.screens.movies.MoviesConstants
import kotlinx.coroutines.launch

/**
 * Landscape Movie Row Carousel (Phase 4 Target Design).
 * Renders horizontal 16:9 landscape carousels directly below Collections,
 * with deterministic FocusRequester navigation and focus memory.
 */
@Composable
fun MovieLandscapeRow(
    rowId: String,
    title: String,
    movies: List<Movie>,
    onMovieFocused: (String, Movie) -> Unit,
    onMovieSelected: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = true,
    navFocusRequester: FocusRequester? = null,
    getRequester: ((String, Int) -> FocusRequester)? = null,
    getNextRowRequester: ((Int) -> FocusRequester?)? = null,
    getPrevRowRequester: ((Int) -> FocusRequester?)? = null,
    focusMemoryManager: FocusMemoryManager? = null
) {
    if (movies.isEmpty()) return

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val defaultRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Section Header Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(
                start = MoviesConstants.CONTENT_START_PADDING,
                bottom = 12.dp
            )
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Horizontal Landscape Carousel
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = MoviesConstants.CONTENT_START_PADDING,
                end = MoviesConstants.CONTENT_END_PADDING
            ),
            horizontalArrangement = Arrangement.spacedBy(MoviesConstants.LANDSCAPE_CARD_SPACING)
        ) {
            itemsIndexed(
                items = movies,
                key = { _, movie -> "${rowId}_${movie.movieId}" }
            ) { index, movie ->
                val cardRequester = getRequester?.invoke(rowId, index) ?: defaultRequester
                val prevCardRequester = if (index > 0) getRequester?.invoke(rowId, index - 1) else null
                val nextCardRequester = if (index < movies.size - 1) getRequester?.invoke(rowId, index + 1) else null

                val upRequester = getPrevRowRequester?.invoke(index)
                val downRequester = getNextRowRequester?.invoke(index)

                MovieLandscapeCard(
                    movie = movie,
                    focusRequester = cardRequester,
                    modifier = Modifier.focusProperties {
                        left = if (index == 0) (navFocusRequester ?: left) else (prevCardRequester ?: left)
                        right = nextCardRequester ?: right
                        if (upRequester != null) up = upRequester
                        if (downRequester != null) down = downRequester
                    },
                    onFocus = { focusedMovie ->
                        focusMemoryManager?.saveFocus(rowId, index.toString())
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                        onMovieFocused(rowId, focusedMovie)
                    },
                    onClick = { clickedMovie ->
                        onMovieSelected(clickedMovie)
                        onMovieClick(clickedMovie)
                    }
                )
            }
        }
    }
}
