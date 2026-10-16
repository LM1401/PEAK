package com.example.peak.ui.screens.random

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.MovieCard
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.ui.screens.home.HomeConstants
import kotlinx.coroutines.delay

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
    
    val resultsFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.status) {
        if (uiState.status == RandomStatus.IDLE) {
            try {
                contentFocusRequester.requestFocus()
            } catch (e: Exception) {}
        } else if (uiState.status == RandomStatus.RESULTS && uiState.results.isNotEmpty()) {
            try {
                resultsFocusRequester.requestFocus()
            } catch (e: Exception) {}
        }
    }

    // Provide the first result as the background movie to make it cinematic, 
    // or null if idle.
    val backgroundMovieProvider: @Composable () -> Movie? = {
        if (uiState.status == RandomStatus.RESULTS && uiState.results.isNotEmpty()) {
            uiState.results.first()
        } else null
    }

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.RANDOM,
        onSidebarItemSelected = onSidebarItemSelected,
        focusedMovieProvider = backgroundMovieProvider,
        navFocusRequester = navFocusRequester,
        contentFocusRequester = if (uiState.status == RandomStatus.RESULTS) resultsFocusRequester else contentFocusRequester,
        heroHud = null
    ) { modifier ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = HomeConstants.HOME_CONTENT_START_PADDING,
                    top = 48.dp,
                    end = 48.dp,
                    bottom = 48.dp
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            when (uiState.status) {
                RandomStatus.IDLE -> {
                    IdleStateContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        focusRequester = contentFocusRequester
                    )
                }
                RandomStatus.LOADING -> {
                    Text(
                        text = "Loading PEAK...",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                RandomStatus.REVEALING -> {
                    Text(
                        text = "Finding something great...",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                }
                RandomStatus.RESULTS -> {
                    ResultsStateContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onMovieClick = onMovieClick,
                        focusRequester = resultsFocusRequester
                    )
                }
                RandomStatus.ERROR -> {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Something went wrong: ${uiState.errorMessage}",
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.resetToIdle() },
                            modifier = Modifier.focusRequester(contentFocusRequester)
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun IdleStateContent(
    uiState: RandomUiState,
    viewModel: RandomViewModel,
    focusRequester: FocusRequester
) {
    Column(
        modifier = Modifier.fillMaxWidth(0.6f),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Random Selector",
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Can't decide what to watch? Select your preferred media type and how many titles you want, and we'll pick for you.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Content Type",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        // Mode Selector
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val isMovies = uiState.request.includeMovies && !uiState.request.includeTvShows
            val isTvShows = !uiState.request.includeMovies && uiState.request.includeTvShows
            val isBoth = uiState.request.includeMovies && uiState.request.includeTvShows

            SelectableChip(
                text = "Movies",
                selected = isMovies,
                onClick = { viewModel.setMediaTypes(movies = true, tvShows = false) },
                modifier = Modifier.focusRequester(focusRequester)
            )
            SelectableChip(
                text = "TV Shows",
                selected = isTvShows,
                onClick = { viewModel.setMediaTypes(movies = false, tvShows = true) }
            )
            SelectableChip(
                text = "Both",
                selected = isBoth,
                onClick = { viewModel.setMediaTypes(movies = true, tvShows = true) }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Quantity",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        // Quantity Selector (1-10)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 1..10) {
                SelectableNumberChip(
                    number = i,
                    selected = uiState.selectedQuantity == i,
                    onClick = { viewModel.setQuantity(i) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Primary Action
        Button(
            onClick = { viewModel.generateSelection(skipReveal = false) },
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.height(56.dp),
            contentPadding = PaddingValues(horizontal = 48.dp)
        ) {
            Text(
                text = "PICK SOMETHING",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
            focusedContainerColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
            contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            focusedContentColor = Color.Black
        ),
        shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
        modifier = modifier.height(40.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SelectableNumberChip(
    number: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
            focusedContainerColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
            contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            focusedContentColor = Color.Black
        ),
        shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.small),
        modifier = modifier.size(40.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = number.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ResultsStateContent(
    uiState: RandomUiState,
    viewModel: RandomViewModel,
    onMovieClick: (Movie) -> Unit,
    focusRequester: FocusRequester
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        if (uiState.partialResultWarning != null) {
            Text(
                text = uiState.partialResultWarning,
                color = Color.Yellow,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Text(
            text = "Your Random Selection",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 48.dp)
        ) {
            items(uiState.results.size) { index ->
                val movie = uiState.results[index]
                MovieCard(
                    movie = movie,
                    onFocus = { },
                    onClick = { onMovieClick(movie) },
                    modifier = Modifier
                        .height(300.dp)
                        .aspectRatio(0.66f)
                        .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { viewModel.generateSelection(skipReveal = false) },
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Roll Again", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            Button(
                onClick = { viewModel.resetToIdle() },
                colors = ButtonDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color.White
                )
            ) {
                Text("Change Options", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}
