package com.example.peak.ui.screens.home

import android.util.Log
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
import com.example.peak.ui.focus.rememberFocusMemoryManager
import com.example.peak.ui.image.ImagePreloader

// STANDARDIZED TV LAYOUT CONSTANTS
private val HORIZONTAL_PADDING = 48.dp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTabSelected: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Create focus memory manager for this screen
    val focusManager = rememberFocusMemoryManager()
    
    Log.d("CW_DEBUG", "UI state -> rows=${state.rows.size}, CW_progress_size=${state.continueWatchingProgress.size}")

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
        // 1. BACKGROUND (ambient layer) - Always visible or black
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
            // 3.1 TOP NAVIGATION BAR
            item {
                TopNavigationBar(
                    selectedTab = "Home",
                    onTabSelected = onTabSelected,
                    onSettingsClick = onSettingsClick,
                    onSearchClick = onSearchClick
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
                    onMovieClick = onMovieClick,
                    progressMap = if (row.title == "Continue Watching") state.continueWatchingProgress else null,
                    focusManager = focusManager
                )
            }

            // 3.3 EMPTY STATE FALLBACK
            if (!state.loading && state.rows.isEmpty()) {
                item {
                    EmptyHomePlaceholder()
                }
            }
        }

        // 4. LOADING OVERLAY (Non-blocking)
        if (state.loading && state.rows.isEmpty()) {
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
}

/**
 * Fallback UI when no content is available.
 * Ensures the screen is never blank and remains focusable.
 */
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


