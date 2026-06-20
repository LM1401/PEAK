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
import com.example.peak.ui.components.*
import com.example.peak.ui.components.metadata.MovieMetadataSection
import com.example.peak.ui.focus.rememberFocusMemoryManager
import com.example.peak.ui.image.ImageWarmingManager
import com.example.peak.ui.image.PeakImageLoader

/**
 * RECONSTRUCTED HOMESCREEN
 * Strict layered architecture to resolve focus and layout conflicts.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTabSelected: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val focusedMovie by viewModel.currentFocusedMovie.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val focusedMovieId by viewModel.focusedMovieId.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val continueWatchingProgress by viewModel.continueWatchingProgress.collectAsState()

    val context = LocalContext.current
    val imageLoader = remember { PeakImageLoader.getInstance(context) }
    val focusManager = rememberFocusMemoryManager()
    val contentFocusRequester = remember { FocusRequester() }

    LaunchedEffect(rows) {
        if (rows.isNotEmpty()) {
            val allUrls = rows.take(3).flatMap { row ->
                row.movies.flatMap { listOf(it.imageUrl, it.backdropUrl) }
            }
            ImageWarmingManager.warm(context, imageLoader, allUrls)
        }
    }

    // 1. ROOT LAYOUT
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 2. BACKGROUND LAYER (NON-FOCUS)
        CinematicBackgroundLayer(movie = focusedMovie)

        // 3. CONTENT LAYER (FOCUS SYSTEM)
        Column(modifier = Modifier.fillMaxSize()) {
            // A) TopNavigationBar - Must allow exit DOWN
            TopNavigationBar(
                selectedTab = "Home",
                onTabSelected = onTabSelected,
                onSettingsClick = onSettingsClick,
                onSearchClick = onSearchClick
            )

            // B) LazyColumn (MOVIE ROWS) - Weighted to fill space
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(contentFocusRequester),
                contentPadding = PaddingValues(top = 160.dp), // Room for fixed HeroSection
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = rows,
                    key = { it.id }
                ) { row ->
                    MovieRow(
                        row = row,
                        focusedMovieId = focusedMovieId,
                        onMovieFocused = { id -> viewModel.onMovieFocused(id) },
                        onMovieSelected = viewModel::onMovieSelected,
                        onMovieClick = onMovieClick,
                        progressMap = if (row.id == "continue_watching") continueWatchingProgress else null,
                        focusManager = focusManager
                    )
                }

                if (!loading && rows.isEmpty()) {
                    item { EmptyHomePlaceholder() }
                }
            }
            
            // 4. OVERLAY LAYER (NON-FOCUS / FIXED)
            // Positioned at the bottom of the Column to ensure NO OVERLAP with LazyColumn
            MovieMetadataSection(
                movie = focusedMovie,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .background(Color.Black.copy(alpha = 0.4f)) // Subtle scrim
            )
        }

        // HERO SECTION (Read-only overlay)
        // Positioned top-left, independent of scroll, but below TopNav
        HeroSection(
            movie = focusedMovie,
            modifier = Modifier
                .padding(top = 100.dp, start = 120.dp)
                .align(Alignment.TopStart)
        )

        // LOADING STATE OVERLAY
        if (loading && rows.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().zIndex(20f).background(Color.Black),
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

@Composable
private fun CinematicBackgroundLayer(movie: Movie?) {
    Box(modifier = Modifier.fillMaxSize()) {
        CinematicBackground(
            backdropUrl = movie?.backdropUrl,
            movieId = movie?.movieId,
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
