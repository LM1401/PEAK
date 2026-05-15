package com.example.peak.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.peak.ui.components.sidebar.Sidebar
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.ui.components.MovieCard

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
private fun HeroSection(
    movie: Movie?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.DarkGray), // Base fallback color
        contentAlignment = Alignment.BottomStart
    ) {
        movie?.let { currentMovie ->
            val backdrop = currentMovie.backdropUrl
            val imageUrl = when {
                backdrop.startsWith("/") -> "https://image.tmdb.org/t/p/w1280$backdrop" // Using higher quality for Hero
                backdrop.isNotBlank() -> backdrop
                else -> null
            }

            if (imageUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = currentMovie.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Dual Gradient Overlay for depth and readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )

            // Info Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 32.dp)
                    .fillMaxWidth(0.6f)
            ) {
                Text(
                    text = currentMovie.name,
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata: Year | Duration | Age Rating Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentMovie.year,
                        color = Color.LightGray,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "  •  ",
                        color = Color.Gray
                    )
                    Text(
                        text = currentMovie.duration,
                        color = Color.LightGray,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Age Rating Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = currentMovie.ageRating,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentMovie.description,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Select a movie", color = Color.Gray)
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
        HeroSection(
            movie = heroMovie,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            itemsIndexed(
                rows,
                key = { _, row -> row.title }
            ) { _, row ->

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

            itemsIndexed(
                row.movies,
                key = { _, movie -> movie.movieId }
            ) { _, movie ->

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
