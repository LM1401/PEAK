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
import com.example.peak.ui.image.ImagePreloader

/**
 * Movies Screen. 
 * Refactored for extreme recomposition isolation and shared structural layout.
 */
@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel,
    onTabSelected: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val focusedMovie by viewModel.currentFocusedMovie.collectAsState()

    HomeBaseLayout(
        selectedTab = "Films",
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        onSearchClick = onSearchClick,
        focusedMovie = focusedMovie
    ) { modifier ->
        MoviesRowsLayer(
            viewModel = viewModel,
            onMovieClick = onMovieClick,
            modifier = modifier
        )
    }
}

@Composable
private fun MoviesRowsLayer(
    viewModel: MoviesViewModel,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows by viewModel.rows.collectAsState()
    val focusedMovieId by viewModel.focusedMovieId.collectAsState()
    val loading by viewModel.loading.collectAsState()

    val context = LocalContext.current
    val focusManager = rememberFocusMemoryManager()
    val contentFocusRequester = remember { FocusRequester() }

    LaunchedEffect(rows) {
        if (rows.isNotEmpty()) {
            val allUrls = rows.take(3).flatMap { row ->
                row.movies.flatMap { listOf(it.imageUrl, it.backdropUrl) }
            }
            ImagePreloader.preload(context, allUrls)
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
            contentPadding = PaddingValues(top = 340.dp, bottom = 320.dp)
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
