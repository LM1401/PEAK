package com.example.peak.ui.screens.random

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.MovieCard
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.ui.screens.home.HomeConstants

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RandomScreen(
    viewModel: RandomViewModel,
    onSidebarItemSelected: (SidebarItemType) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val navFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val surpriseMeRequester = remember { FocusRequester() }
    val cardFocusRequester = remember { FocusRequester() }
    
    var isInitialFocusRequested by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.status) {
        if (!isInitialFocusRequested && uiState.status == RandomStatus.IDLE) {
            try {
                surpriseMeRequester.requestFocus()
                isInitialFocusRequested = true
            } catch (e: Exception) {}
        } else if (uiState.status == RandomStatus.RESULTS && uiState.results.isNotEmpty()) {
            try {
                cardFocusRequester.requestFocus()
            } catch (e: Exception) {}
        }
    }

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.RANDOM,
        onSidebarItemSelected = onSidebarItemSelected,
        focusedMovieProvider = { null }, // Optional: Provide focused movie for background
        navFocusRequester = navFocusRequester,
        contentFocusRequester = if (uiState.status == RandomStatus.RESULTS) cardFocusRequester else surpriseMeRequester,
        heroHud = null
    ) { modifier ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = HomeConstants.HOME_CONTENT_START_PADDING,
                    top = 32.dp,
                    end = 48.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            when (uiState.status) {
                RandomStatus.IDLE -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "RANDOM SELECTOR",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { viewModel.generateSelection(skipReveal = false) },
                            modifier = Modifier.focusRequester(surpriseMeRequester)
                        ) {
                            Text("Surprise Me!")
                        }
                        
                        // Add more controls here (Custom Mix, Filters, etc.)
                        // Example: simple Custom Mix for Movies vs TV
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { 
                                val newMovies = (uiState.request.moviesRequested + 1).coerceAtMost(10)
                                viewModel.setCustomMix(newMovies, uiState.request.tvRequested)
                            }) {
                                Text("Movies: ${uiState.request.moviesRequested}")
                            }
                            Button(onClick = { 
                                val newTv = (uiState.request.tvRequested + 1).coerceAtMost(10)
                                viewModel.setCustomMix(uiState.request.moviesRequested, newTv)
                            }) {
                                Text("TV Shows: ${uiState.request.tvRequested}")
                            }
                        }
                    }
                }
                RandomStatus.LOADING -> {
                    Text(
                        text = "Loading PEAK...",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                RandomStatus.REVEALING -> {
                    // Simulated Reveal Animation
                    Text(
                        text = "Finding something great...",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                }
                RandomStatus.RESULTS -> {
                    Column {
                        if (uiState.partialResultWarning != null) {
                            Text(
                                text = uiState.partialResultWarning!!,
                                color = Color.Yellow,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(uiState.results) { index, movie ->
                                MovieCard(
                                    movie = movie,
                                    onFocus = { },
                                    onClick = { onMovieClick(movie) },
                                    modifier = Modifier
                                        .aspectRatio(0.66f)
                                        .then(if (index == 0) Modifier.focusRequester(cardFocusRequester) else Modifier)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { viewModel.generateSelection(skipReveal = false) }) {
                                Text("Roll Again")
                            }
                            Button(onClick = { viewModel.resetToIdle() }) {
                                Text("Customise")
                            }
                        }
                    }
                }
                RandomStatus.ERROR -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Something went wrong: ${uiState.errorMessage}",
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.resetToIdle() },
                            modifier = Modifier.focusRequester(surpriseMeRequester)
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}
