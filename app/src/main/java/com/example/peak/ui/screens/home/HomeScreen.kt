package com.example.peak.ui.screens.home

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

// STANDARDIZED TV LAYOUT CONSTANTS
private val HORIZONTAL_PADDING = 48.dp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMovieClick: (Movie) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
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
                        TopNavigationBar()
                    }

                    // 3.2 MOVIE ROWS & CONTEXTUAL INFO
                    itemsIndexed(
                        items = state.rows,
                        key = { _, row -> row.title }
                    ) { index, row ->
                        
                        MovieRow(
                            row = row,
                            viewModel = viewModel,
                            onMovieClick = onMovieClick
                        )

                        // INTEGRATE INFO BETWEEN ROWS: 
                        // If this is the first row, show metadata block directly beneath.
                        // Matches image 1 where info is below "Coming Soon" row.
                        if (index == 0 && state.selectedMovie != null) {
                            HeroSection(
                                movie = state.selectedMovie,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieRow(
    row: Row,
    viewModel: HomeViewModel,
    onMovieClick: (Movie) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = row.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 120.dp, bottom = 12.dp) // Aligned with metadata
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 120.dp), // NETFLIX 2024 GRID: 120dp
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = row.movies,
                key = { it.movieId }
            ) { movie ->
                MovieCard(
                    movie = movie,
                    onFocus = { focusedMovie -> 
                        focusedMovie?.let { viewModel.onMovieFocused(it) }
                    },
                    onClick = { 
                        viewModel.onMovieSelected(movie)
                        onMovieClick(movie)
                    }
                )
            }
        }
    }
}
