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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.peak.domain.model.Movie
import com.example.peak.ui.focus.FocusMemoryManager
import com.example.peak.ui.screens.movies.MoviesConstants
import com.example.peak.ui.screens.movies.Top10SectionState
import kotlinx.coroutines.launch

/**
 * Top 10 Ranked Movie Carousel (Phase 5 Target Design).
 * Renders horizontal carousel with stylized "Top 10 in the UK Today >" header,
 * deterministic FocusRequester chaining for 1-10 horizontal accessibility, and focus memory support.
 */
@Composable
fun Top10Row(
    top10State: Top10SectionState,
    onMovieFocused: (String, Movie) -> Unit,
    onMovieSelected: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    rowId: String = "movies_top10",
    navFocusRequester: FocusRequester? = null,
    getRequester: ((String, Int) -> FocusRequester)? = null,
    getNextRowRequester: ((Int) -> FocusRequester?)? = null,
    getPrevRowRequester: ((Int) -> FocusRequester?)? = null,
    focusMemoryManager: FocusMemoryManager? = null
) {
    val items = top10State.items
    if (items.isEmpty() && !top10State.isLoading) return

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val defaultRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Section Header Row ("Top 10 in the UK Today >")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(
                start = MoviesConstants.CONTENT_START_PADDING,
                bottom = 12.dp
            )
        ) {
            val titleText = buildAnnotatedString {
                append("Top 10 in the ")
                withStyle(
                    style = SpanStyle(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                ) {
                    append("UK Today")
                }
            }

            Text(
                text = titleText,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Horizontal Top 10 Carousel
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = MoviesConstants.CONTENT_START_PADDING,
                end = MoviesConstants.CONTENT_END_PADDING
            ),
            horizontalArrangement = Arrangement.spacedBy(MoviesConstants.TOP10_CARD_SPACING)
        ) {
            itemsIndexed(
                items = items,
                key = { _, rankedMovie -> "${rowId}_${rankedMovie.movie.movieId}" }
            ) { index, rankedMovie ->
                val cardRequester = getRequester?.invoke(rowId, index) ?: defaultRequester
                val prevCardRequester = if (index > 0) getRequester?.invoke(rowId, index - 1) else null
                val nextCardRequester = if (index < items.size - 1) getRequester?.invoke(rowId, index + 1) else null

                val upRequester = getPrevRowRequester?.invoke(index)
                val downRequester = getNextRowRequester?.invoke(index)

                Top10Card(
                    rankedMovie = rankedMovie,
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
