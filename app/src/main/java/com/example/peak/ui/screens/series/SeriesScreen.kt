package com.example.peak.ui.screens.series

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.MovieRow
import com.example.peak.ui.focus.rememberFocusMemoryManager
import com.example.peak.ui.image.ImageWarmingManager
import com.example.peak.ui.image.PeakImageLoader

/**
 * Series Screen. 
 * Optimized for synchronous focus response and zero-lag rendering.
 */
@Composable
fun SeriesScreen(
    viewModel: SeriesViewModel,
    onTabSelected: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val focusState by viewModel.focusState.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val loading by viewModel.loading.collectAsState()

    val context = LocalContext.current
    val imageLoader = remember { PeakImageLoader.getInstance(context) }
    val focusManager = rememberFocusMemoryManager()
    val contentFocusRequester = remember { FocusRequester() }
    val navFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // SYNC: Ensure LazyColumn scrolls to the focused row on first entry or focus change
    LaunchedEffect(focusState?.movieId) {
        if (focusState != null && rows.isNotEmpty()) {
            val focusedRowIndex = rows.indexOfFirst { row -> 
                row.movies.any { it.movieId == focusState?.movieId } 
            }
            if (focusedRowIndex != -1) {
                listState.animateScrollToItem(focusedRowIndex)
            }
        }
    }

    LaunchedEffect(rows) {
        if (rows.isNotEmpty()) {
            val moviesToWarm = rows.take(3).flatMap { it.movies }
            ImageWarmingManager.warm(context, imageLoader, moviesToWarm)
        }
    }

    HomeBaseLayout(
        selectedTab = "Series",
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        onSearchClick = onSearchClick,
        focusedMovie = focusState?.movie,
        showLoadingOverlay = loading && rows.isEmpty(),
        navFocusRequester = navFocusRequester,
        contentFocusRequester = contentFocusRequester
    ) { modifier ->
        LazyColumn(
            state = listState,
            modifier = modifier
                .focusRequester(contentFocusRequester)
                .focusProperties {
                    up = navFocusRequester
                },
            verticalArrangement = Arrangement.spacedBy(48.dp), 
            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
        ) {
            items(
                items = rows,
                key = { it.id }
            ) { row ->
                MovieRow(
                    row = row,
                    focusedMovieId = focusState?.movieId,
                    onMovieFocused = { id -> 
                        row.movies.find { it.movieId == id }?.let(viewModel::onMovieFocused)
                    },
                    onMovieSelected = viewModel::onMovieSelected,
                    onMovieClick = onMovieClick,
                    focusManager = focusManager
                )
            }

            if (!loading && rows.isEmpty()) {
                item {
                    EmptySeriesPlaceholder()
                }
            }
        }
    }
}

@Composable
fun EmptySeriesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(horizontal = 120.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = "No series found.",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try again later or check back for new additions.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
