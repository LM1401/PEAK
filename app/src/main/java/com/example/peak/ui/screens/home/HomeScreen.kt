package com.example.peak.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.MovieRow
import com.example.peak.ui.focus.rememberFocusMemoryManager
import com.example.peak.ui.image.ImageWarmingManager
import com.example.peak.ui.image.PeakImageLoader

/**
 * HomeScreen.
 * Optimized for frame-perfect focus response by collecting a single FocusState.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTabSelected: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    // SINGLE COLLECTOR: Synchronizes Background, Hero, and Metadata in a single frame.
    val focusState by viewModel.focusState.collectAsState()
    
    val rows by viewModel.rows.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val continueWatchingProgress by viewModel.continueWatchingProgress.collectAsState()

    val context = LocalContext.current
    val imageLoader = remember { PeakImageLoader.getInstance(context) }
    val focusManager = rememberFocusMemoryManager()
    val contentFocusRequester = remember { FocusRequester() }

    LaunchedEffect(rows) {
        if (rows.isNotEmpty()) {
            val moviesToWarm = rows.take(3).flatMap { it.movies }
            ImageWarmingManager.warm(context, imageLoader, moviesToWarm)
        }
    }

    HomeBaseLayout(
        selectedTab = "Home",
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        onSearchClick = onSearchClick,
        focusedMovie = focusState?.movie,
        showLoadingOverlay = loading && rows.isEmpty()
    ) { modifier ->
        LazyColumn(
            modifier = modifier.focusRequester(contentFocusRequester),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(
                items = rows,
                key = { it.id }
            ) { row ->
                MovieRow(
                    row = row,
                    focusedMovieId = focusState?.movieId,
                    onMovieFocused = { id -> viewModel.onMovieFocused(id) },
                    onMovieSelected = viewModel::onMovieSelected,
                    onMovieClick = onMovieClick,
                    progressMap = if (row.id == "continue_watching") continueWatchingProgress else null,
                    focusManager = focusManager
                )
            }

            if (!loading && rows.isEmpty()) {
                item {
                    EmptyHomePlaceholder()
                }
            }
        }
    }
}

@Composable
fun EmptyHomePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(horizontal = 120.dp),
        contentAlignment = androidx.compose.ui.Alignment.CenterStart
    ) {
        androidx.compose.foundation.layout.Column {
            androidx.tv.material3.Text(
                text = "No content available right now.",
                style = androidx.tv.material3.MaterialTheme.typography.headlineSmall,
                color = androidx.compose.ui.graphics.Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            androidx.tv.material3.Text(
                text = "Try checking your internet connection or come back later.",
                style = androidx.tv.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.Gray
            )
        }
    }
}
