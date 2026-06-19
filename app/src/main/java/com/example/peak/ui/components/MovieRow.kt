package com.example.peak.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.ui.focus.FocusMemoryManager
import kotlinx.coroutines.delay

/**
 * Reusable Movie Row component for TV browsing screens.
 * Enhanced with Focus Memory to remember last focused items.
 */
@Composable
fun MovieRow(
    row: Row,
    onMovieFocused: (Movie) -> Unit,
    onMovieSelected: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    progressMap: Map<String, Float>? = null,
    focusManager: FocusMemoryManager? = null
) {
    // Local focus state
    var isRowFocused by remember { mutableStateOf(false) }
    
    // Track the raw focus ID for immediate scaling feedback
    // Initialize from memory if available
    var rawFocusedMovieId by remember { 
        mutableStateOf<String?>(focusManager?.getRememberedId(row.title)) 
    }
    
    // Requesters map for focus restoration
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    // Restore focus when the row is re-entered
    LaunchedEffect(isRowFocused) {
        if (isRowFocused && rawFocusedMovieId != null) {
            focusRequesters[rawFocusedMovieId]?.requestFocus()
        }
    }

    // Persist focus to manager
    LaunchedEffect(rawFocusedMovieId) {
        rawFocusedMovieId?.let { id ->
            focusManager?.saveFocus(row.title, id)
        }
    }
    
    // Debounce the expansion state to prevent layout jitter during fast scrolling
    var settledFocusedMovieId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(rawFocusedMovieId) {
        if (rawFocusedMovieId == null) {
            settledFocusedMovieId = null
        } else {
            // Wait for focus to settle before triggering heavy layout changes or metadata
            delay(80) 
            settledFocusedMovieId = rawFocusedMovieId
        }
    }

    val focusedMovie = row.movies.find { it.movieId == settledFocusedMovieId }

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
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 120.dp, bottom = 12.dp)
        )

        LazyRow(
            modifier = Modifier.height(300.dp), 
            contentPadding = PaddingValues(horizontal = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(
                items = row.movies,
                key = { it.movieId }
            ) { movie ->
                val isSettledFocused = settledFocusedMovieId == movie.movieId
                
                // 350ms duration for high-end TV responsiveness
                val transitionDuration = 350
                val easing = FastOutSlowInEasing

                val cardWidth by animateDpAsState(
                    targetValue = if (isSettledFocused) 420.dp else 180.dp,
                    animationSpec = tween(transitionDuration, easing = easing),
                    label = "cardWidth"
                )

                MovieCard(
                    movie = movie,
                    isSettled = isSettledFocused, // Sync image swap with expansion
                    modifier = Modifier
                        .width(cardWidth) 
                        .height(270.dp)
                        .focusRequester(focusRequesters.getOrPut(movie.movieId) { FocusRequester() }),
                    progress = progressMap?.get(movie.movieId),
                    onFocus = { focused -> 
                        if (focused != null) {
                            rawFocusedMovieId = movie.movieId
                            onMovieFocused(focused)
                        }
                    },
                    onClick = { 
                        onMovieSelected(movie)
                        onMovieClick(movie)
                    }
                )
            }
        }

        // Use a consistent duration for metadata appearance
        val metaTransitionDuration = 350

        // METADATA UNDER THE CARD (Synced with settled focus)
        androidx.compose.animation.AnimatedVisibility(
            visible = isRowFocused && focusedMovie != null,
            enter = androidx.compose.animation.expandVertically(
                animationSpec = tween(metaTransitionDuration, easing = FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeIn(animationSpec = tween(metaTransitionDuration)),
            exit = androidx.compose.animation.shrinkVertically(
                animationSpec = tween(metaTransitionDuration, easing = FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeOut(animationSpec = tween(metaTransitionDuration))
        ) {
            focusedMovie?.let { movie ->
                HeroSection(
                    movie = movie,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
    }
}
