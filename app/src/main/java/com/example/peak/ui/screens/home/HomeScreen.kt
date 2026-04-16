package com.example.peak.ui.screens.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.tv.material3.*
import coil.compose.rememberAsyncImagePainter
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.ui.components.MovieCard

/**
 * The main Home Screen of the app.
 * It observes state from the [HomeViewModel].
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMovieClick: (Movie) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusedMovie by viewModel.focusedMovie.collectAsState()

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Text("Loading PEAK...", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            }
        }
        is HomeUiState.Success -> {
            HomeContent(
                rows = state.rows,
                focusedMovie = focusedMovie,
                onMovieFocused = { viewModel.onMovieFocused(it) },
                onMovieClick = onMovieClick
            )
        }
        is HomeUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Text(text = state.message, color = Color.White)
            }
        }
    }
}

@Composable
private fun HomeContent(
    rows: List<Row>,
    focusedMovie: Movie?,
    onMovieFocused: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Hero Section
        BoxWithConstraints {
            val heroHeight = maxHeight * 0.35f
            HeroSection(
                movie = focusedMovie,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Scrollable movie rows
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            itemsIndexed(rows) { _, row ->
                MovieRow(
                    row = row,
                    onMovieFocused = onMovieFocused,
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroSection(
    movie: Movie?,
    modifier: Modifier = Modifier
) {
    movie?.let { currentMovie ->
        Box(
            modifier = modifier
                .focusable(false)
        ) {
            // Hero image with Crossfade for smooth transitions when focusing different movies
            Crossfade(targetState = currentMovie, animationSpec = tween(800)) { target ->
                Image(
                    painter = rememberAsyncImagePainter(target.backdropUrl),
                    contentDescription = target.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Left gradient overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Bottom gradient overlay to blend into the list
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Movie info (title + description)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 24.dp)
                    .fillMaxWidth(0.7f)
            ) {
                Text(
                    text = currentMovie.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentMovie.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Age rating badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = currentMovie.ageRating,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MovieRow(
    row: Row, 
    onMovieFocused: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = row.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(start = 58.dp, bottom = 8.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 58.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(row.movies) { _, movie ->
                MovieCard(
                    movie = movie,
                    onMovieFocused = onMovieFocused,
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}
