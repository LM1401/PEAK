package com.example.peak.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.ui.components.MovieCard
import com.example.peak.ui.components.HeroSection
import com.example.peak.ui.components.CinematicBackground
import com.example.peak.ui.components.HomeGradientsOverlay
import com.example.peak.ui.components.TopNavigationBar
import com.example.peak.ui.components.MovieRow
import com.example.peak.ui.image.ImagePreloader

// STANDARDIZED TV LAYOUT CONSTANTS
private val HORIZONTAL_PADDING = 48.dp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTabSelected: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Preload first few rows on initial launch to ensure buttery scrolling
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
                    text = "Loading...",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        if (state.rows.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                
                // 1. BACKGROUND (ambient layer)
                CinematicBackground(
                    backdropUrl = state.selectedMovie?.backdropUrl,
                    modifier = Modifier.fillMaxSize().zIndex(0f)
                )

                // 2. GRADIENT DEPTH
                HomeGradientsOverlay(
                    modifier = Modifier.fillMaxSize().zIndex(1f)
                )

                // 3. MAIN UI SCROLL (Continuous Canvas)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                        .focusRequester(contentFocusRequester),
                    contentPadding = PaddingValues(bottom = 64.dp)
                ) {
                    // 3.1 TOP NAVIGATION BAR (Matches image task bar)
                    item {
                        TopNavigationBar(
                            selectedTab = "Home",
                            onTabSelected = onTabSelected,
                            onSettingsClick = onSettingsClick
                        )
                    }

                    // 3.2 MOVIE ROWS (Focus-driven metadata)
                    itemsIndexed(
                        items = state.rows,
                        key = { _, row -> row.title }
                    ) { _, row ->
                        
                        MovieRow(
                            row = row,
                            onMovieFocused = { viewModel.onMovieFocused(it) },
                            onMovieSelected = { viewModel.onMovieSelected(it) },
                            onMovieClick = onMovieClick
                        )
                    }
                }
            }
        }
    }
}


