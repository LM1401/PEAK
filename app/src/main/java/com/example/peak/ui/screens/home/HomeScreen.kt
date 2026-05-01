package com.example.peak.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import coil.compose.rememberAsyncImagePainter
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.ui.components.HomeMovieCard

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMovieClick: (Movie) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // SINGLE SOURCE OF TRUTH (UI owns focus)
    var focusedMovie by remember { mutableStateOf<Movie?>(null) }

    when (val state = uiState) {

        is HomeUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading...",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        is HomeUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    color = Color.White
                )
            }
        }

        is HomeUiState.Success -> {
            // AUTO-FOCUS FIRST MOVIE ON LOAD
            LaunchedEffect(state.rows) {
                if (focusedMovie == null) {
                    state.rows.firstOrNull()?.movies?.firstOrNull()?.let { firstMovie ->
                        focusedMovie = firstMovie
                    }
                }
            }

            HomeContent(
                rows = state.rows,
                focusedMovie = focusedMovie,
                onFocusChange = { movie ->
                    if (focusedMovie != movie) {
                        focusedMovie = movie
                    }
                },
                onMovieClick = onMovieClick
            )
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
                backdrop.startsWith("/") -> "https://image.tmdb.org/t/p/w780$backdrop"
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

            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            Text(
                text = currentMovie.name,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 32.dp, bottom = 32.dp)
            )
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
    focusedMovie: Movie?,
    onFocusChange: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // HERO (Netflix-style top banner)
        HeroSection(
            movie = focusedMovie,
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
                    onFocusChange = onFocusChange,
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}

@Composable
private fun MovieRow(
    row: Row,
    onFocusChange: (Movie) -> Unit,
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
                key = { _, movie -> movie.movieId } // FIXED: movie.id -> movie.movieId
            ) { _, movie ->

                HomeMovieCard(
                    movie = movie,
                    onMovieFocused = { focused ->
                        onFocusChange(focused) // ONLY UI STATE UPDATE
                    },
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}