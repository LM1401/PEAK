package com.example.peak.ui.screens.movies

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.zIndex
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.MovieRow
import com.example.peak.ui.focus.rememberFocusMemoryManager
import com.example.peak.ui.image.ImageWarmingManager
import com.example.peak.ui.image.PeakImageLoader

/**
 * Movies Screen. 
 * Optimized for synchronous focus response and zero-lag rendering.
 */
@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel,
    onTabSelected: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val focusState by viewModel.focusState.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val loading by viewModel.loading.collectAsState()

    HomeBaseLayout(
        selectedTab = "Films",
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        onSearchClick = onSearchClick,
        focusedMovie = focusState?.movie,
        showLoadingOverlay = loading && rows.isEmpty()
    ) { modifier ->
        MoviesRowsLayer(
            viewModel = viewModel,
            onMovieClick = onMovieClick,
            focusedMovieId = focusState?.movieId,
            modifier = modifier
        )
    }
}

@Composable
private fun MoviesRowsLayer(
    viewModel: MoviesViewModel,
    onMovieClick: (Movie) -> Unit,
    focusedMovieId: String?,
    modifier: Modifier = Modifier
) {
    val rows by viewModel.rows.collectAsState()
    val loading by viewModel.loading.collectAsState()

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

    if (loading && rows.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Loading Movies...",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }

    if (rows.isNotEmpty()) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .zIndex(2f)
                .focusRequester(contentFocusRequester),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(
                items = rows,
                key = { it.id }
            ) { row ->
                MovieRow(
                    row = row,
                    focusedMovieId = focusedMovieId,
                    onMovieFocused = { id -> 
                        row.movies.find { it.movieId == id }?.let(viewModel::onMovieFocused)
                    },
                    onMovieSelected = viewModel::onMovieSelected,
                    onMovieClick = onMovieClick,
                    focusManager = focusManager
                )
            }
        }
    }
}
