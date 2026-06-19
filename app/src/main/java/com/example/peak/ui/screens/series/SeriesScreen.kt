package com.example.peak.ui.screens.series

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
 * Series Screen. 
 * Replicates HomeScreen layout exactly but for Series content.
 */
@Composable
fun SeriesScreen(
    viewModel: SeriesViewModel,
    onTabSelected: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val focusManager = rememberFocusMemoryManager()

    // Preload first few rows for smooth scrolling
    LaunchedEffect(state.rows) {
        if (state.rows.isNotEmpty()) {
            val allUrls = state.rows.take(3).flatMap { row ->
                row.movies.flatMap { listOf(it.imageUrl, it.backdropUrl) }
            }
            ImagePreloader.preload(context, allUrls)
        }
    }

    val contentFocusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading Series...",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        if (state.rows.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                
                CinematicBackground(
                    backdropUrl = state.selectedMovie?.backdropUrl,
                    modifier = Modifier.fillMaxSize().zIndex(0f)
                )

                HomeGradientsOverlay(
                    modifier = Modifier.fillMaxSize().zIndex(1f)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                        .focusRequester(contentFocusRequester),
                    contentPadding = PaddingValues(bottom = 64.dp)
                ) {
                    item {
                        TopNavigationBar(
                            selectedTab = "Series",
                            onTabSelected = onTabSelected,
                            onSettingsClick = onSettingsClick,
                            onSearchClick = onSearchClick
                        )
                    }

                    itemsIndexed(
                        items = state.rows,
                        key = { _, row -> row.title }
                    ) { _, row ->
                        MovieRow(
                            row = row,
                            onMovieFocused = { viewModel.onMovieFocused(it) },
                            onMovieSelected = { viewModel.onMovieSelected(it) },
                            onMovieClick = onMovieClick,
                            focusManager = focusManager
                        )
                    }
                }
            }
        }
    }
}
