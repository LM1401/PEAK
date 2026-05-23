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
import com.example.peak.ui.image.ImagePreloader

// STANDARDIZED TV LAYOUT CONSTANTS
private val HORIZONTAL_PADDING = 48.dp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMovieClick: (Movie) -> Unit
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
                        TopNavigationBar()
                    }

                    // 3.2 MOVIE ROWS (Focus-driven metadata)
                    itemsIndexed(
                        items = state.rows,
                        key = { _, row -> row.title }
                    ) { index, row ->
                        
                        MovieRow(
                            row = row,
                            viewModel = viewModel,
                            onMovieClick = onMovieClick
                        )
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
    // Track the raw focus ID for immediate scaling feedback
    var rawFocusedMovieId by remember { mutableStateOf<String?>(null) }
    
    // Debounce the expansion state to prevent layout jitter during fast scrolling
    var settledFocusedMovieId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(rawFocusedMovieId) {
        if (rawFocusedMovieId == null) {
            settledFocusedMovieId = null
        } else {
            // SNAPPY: Reduced delay to 80ms for instant feel while protecting focus engine
            kotlinx.coroutines.delay(80) 
            settledFocusedMovieId = rawFocusedMovieId
        }
    }

    val focusedMovie = row.movies.find { it.movieId == settledFocusedMovieId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .onFocusChanged { focusState ->
                // Clear the focused movie if the entire row loses focus
                if (!focusState.hasFocus) {
                    rawFocusedMovieId = null
                }
            }
    ) {
        Text(
            text = row.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 120.dp, bottom = 12.dp)
        )

        LazyRow(
            modifier = Modifier.height(300.dp), 
            contentPadding = PaddingValues(horizontal = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(
                items = row.movies,
                key = { it.movieId }
            ) { movie ->
                val isSettledFocused = settledFocusedMovieId == movie.movieId
                
                // SNAPPY: 350ms duration for high-end TV responsiveness
                val transitionDuration = 350
                val easing = FastOutSlowInEasing

                val cardWidth by animateDpAsState(
                    targetValue = if (isSettledFocused) 420.dp else 180.dp,
                    animationSpec = tween(transitionDuration, easing = easing),
                    label = "cardWidth"
                )

                MovieCard(
                    movie = movie,
                    isSettled = isSettledFocused, // Sync image swap with expansion
                    modifier = Modifier
                        .width(cardWidth) 
                        .height(270.dp),
                    onFocus = { focused -> 
                        if (focused != null) {
                            rawFocusedMovieId = movie.movieId
                            viewModel.onMovieFocused(focused)
                        }
                    },
                    onClick = { 
                        viewModel.onMovieSelected(movie)
                        onMovieClick(movie)
                    }
                )
            }
        }

        // Use a consistent duration for metadata appearance
        val metaTransitionDuration = 350

        // METADATA UNDER THE CARD (Synced with settled focus)
        androidx.compose.animation.AnimatedVisibility(
            visible = focusedMovie != null,
            enter = androidx.compose.animation.expandVertically(
                animationSpec = tween(metaTransitionDuration, easing = FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeIn(animationSpec = tween(metaTransitionDuration)),
            exit = androidx.compose.animation.shrinkVertically(
                animationSpec = tween(metaTransitionDuration, easing = FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeOut(animationSpec = tween(metaTransitionDuration))
        ) {
            focusedMovie?.let { movie ->
                HeroSection(
                    movie = movie,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
    }
}
