package com.example.peak.ui.screens.home

import androidx.compose.foundation.background
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
import com.example.peak.ui.components.CinematicBackground
import com.example.peak.ui.components.HomeGradientsOverlay
import com.example.peak.ui.components.TopNavigationBar
import com.example.peak.ui.components.MovieRow
import com.example.peak.ui.focus.rememberFocusMemoryManager
import com.example.peak.ui.image.ImagePreloader

/**
 * FIX 1 — REMOVE HOMESCREEN RECOMPOSITION HUB
 * Refactored into Layered components to isolate state changes.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTabSelected: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HomeBackgroundLayer(viewModel)
        HomeRowsLayer(
            viewModel = viewModel,
            onTabSelected = onTabSelected,
            onMovieClick = onMovieClick,
            onSettingsClick = onSettingsClick,
            onSearchClick = onSearchClick
        )
    }
}

@Composable
private fun HomeBackgroundLayer(viewModel: HomeViewModel) {
    val backdropUrl by viewModel.focusedMovieBackdropUrl.collectAsState()

    CinematicBackgroundLayer(backdropUrl = backdropUrl)
}

@Composable
private fun HomeRowsLayer(
    viewModel: HomeViewModel,
    onTabSelected: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    val rows by viewModel.rows.collectAsState()
    val focusedMovieId by viewModel.focusedMovieId.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val continueWatchingProgress by viewModel.continueWatchingProgress.collectAsState()

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(2f)
            .focusRequester(contentFocusRequester),
        contentPadding = PaddingValues(bottom = 64.dp)
    ) {
        item {
            TopNavigationBar(
                selectedTab = "Home",
                onTabSelected = onTabSelected,
                onSettingsClick = onSettingsClick,
                onSearchClick = onSearchClick
            )
        }

        items(
            items = rows,
            key = { it.id }
        ) { row ->
            MovieRow(
                row = row,
                focusedMovieId = focusedMovieId,
                onMovieFocused = { movie -> movie?.let(viewModel::onMovieFocused) },
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

    if (loading && rows.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().zIndex(3f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Loading PEAK...",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
private fun CinematicBackgroundLayer(backdropUrl: String?) {
    Box(modifier = Modifier.fillMaxSize()) {
        CinematicBackground(
            backdropUrl = backdropUrl,
            modifier = Modifier.fillMaxSize().zIndex(0f)
        )
        HomeGradientsOverlay(
            modifier = Modifier.fillMaxSize().zIndex(1f)
        )
    }
}

@Composable
fun EmptyHomePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(horizontal = 120.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = "No content available right now.",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try checking your internet connection or come back later.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
