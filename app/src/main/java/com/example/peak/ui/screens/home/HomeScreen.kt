package com.example.peak.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.zIndex
import com.example.peak.ui.components.sidebar.Sidebar
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.ui.components.MovieCard
import com.example.peak.ui.components.HeroSection

// CLEAN LAYOUT CONSTANTS
private val HERO_HEIGHT = 420.dp
private val HORIZONTAL_PADDING = 48.dp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMovieClick: (Movie) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedItem by remember { mutableStateOf(SidebarItemType.HOME) }
    val contentFocusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
            // Root Box for Layering
            Box(modifier = Modifier.fillMaxSize()) {
                
                // 1. HERO LAYER (Fixed Header)
                key(state.selectedMovie?.movieId) {
                    HeroSection(
                        movie = state.selectedMovie,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HERO_HEIGHT)
                    )
                }

                // 2. CONTENT LAYER (Starts below Hero)
                HomeContent(
                    rows = state.rows,
                    viewModel = viewModel,
                    onMovieClick = onMovieClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = HERO_HEIGHT)
                        .focusRequester(contentFocusRequester)
                )

                // 3. SIDEBAR OVERLAY
                Sidebar(
                    selectedItem = selectedItem,
                    onItemSelected = { selectedItem = it },
                    onMoveRight = {
                        contentFocusRequester.requestFocus()
                    },
                    modifier = Modifier.zIndex(2f)
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    rows: List<Row>,
    viewModel: HomeViewModel,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.background(Color.Transparent),
        contentPadding = PaddingValues(bottom = HORIZONTAL_PADDING)
    ) {
        items(
            items = rows,
            key = { it.title }
        ) { row ->
            MovieRow(
                row = row,
                viewModel = viewModel,
                onMovieClick = onMovieClick
            )
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
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = row.title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = HORIZONTAL_PADDING, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING),
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
