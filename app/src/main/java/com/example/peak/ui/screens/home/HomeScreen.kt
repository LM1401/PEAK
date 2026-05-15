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
import com.example.peak.ui.components.sidebar.Sidebar
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.ui.components.MovieCard
import com.example.peak.ui.components.HeroSection

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMovieClick: (Movie) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedItem by remember { mutableStateOf(SidebarItemType.HOME) }
    val contentFocusRequester = remember { FocusRequester() }
    
    // UI-ONLY Focus Management
    val focusManager = remember { HomeFocusManager() }

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
            Row(modifier = Modifier.fillMaxSize()) {
                Sidebar(
                    selectedItem = selectedItem,
                    onItemSelected = { selectedItem = it },
                    onMoveRight = {
                        contentFocusRequester.requestFocus()
                    }
                )

                HomeContent(
                    rows = state.rows,
                    focusManager = focusManager,
                    viewModel = viewModel,
                    onMovieClick = onMovieClick,
                    modifier = Modifier.focusRequester(contentFocusRequester)
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    rows: List<Row>,
    focusManager: HomeFocusManager,
    viewModel: HomeViewModel,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val heroMovie = focusManager.focusedMovie
        ?: state.selectedMovie
        ?: rows.firstOrNull()?.movies?.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // HERO (Follows UI-only focus with safe fallbacks)
        key(heroMovie?.movieId) {
            HeroSection(
                movie = heroMovie,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            items(
                items = rows,
                key = { it.title }
            ) { row ->

                MovieRow(
                    row = row,
                    focusManager = focusManager,
                    viewModel = viewModel,
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}

@Composable
private fun MovieRow(
    row: Row,
    focusManager: HomeFocusManager,
    viewModel: HomeViewModel,
    onMovieClick: (Movie) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {

        Text(
            text = row.title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(
                items = row.movies,
                key = { it.movieId }
            ) { movie ->

                MovieCard(
                    movie = movie,
                    onFocus = { focusManager.onFocus(it) },
                    onClick = { 
                        viewModel.onMovieSelected(it)
                        onMovieClick(it)
                    }
                )
            }
        }
    }
}
