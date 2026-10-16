package com.example.peak.ui.screens.random

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.DetailMovieCard
import com.example.peak.ui.components.HeroSection
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.sidebar.CasinoDiceIcon
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
    
    var focusedMovie by remember { mutableStateOf<Movie?>(null) }

    // Auto-fetch on entry if empty
    LaunchedEffect(Unit) {
        if (uiState.results.isEmpty() && uiState.status != RandomStatus.LOADING) {
            viewModel.generateSelection(skipReveal = true)
        }
    }

    // Default focusedMovie to the first result if currently null or not in list
    LaunchedEffect(uiState.results) {
        if (uiState.results.isNotEmpty() && !uiState.results.contains(focusedMovie)) {
            focusedMovie = uiState.results.first()
        }
    }

    // Pass focus initially to the content (Movies chip)
    LaunchedEffect(uiState.status) {
        if (uiState.status == RandomStatus.IDLE) {
            try {
                contentFocusRequester.requestFocus()
            } catch (e: Exception) {}
        }
    }

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.RANDOM,
        onSidebarItemSelected = onSidebarItemSelected,
        focusedMovieProvider = { focusedMovie },
        navFocusRequester = navFocusRequester,
        contentFocusRequester = contentFocusRequester,
        heroHud = {} // Rendered inline below
    ) { modifier ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = HomeConstants.HOME_CONTENT_START_PADDING,
                    top = 24.dp,
                    end = 48.dp,
                    bottom = 16.dp
                ),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // Header
                Text(
                    text = "Random",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Not sure what to watch?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Get a random movie or TV show based on your preferences.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Controls Row 1: Content Type
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val isMovies = uiState.request.includeMovies && !uiState.request.includeTvShows
                    val isTvShows = !uiState.request.includeMovies && uiState.request.includeTvShows
                    val isBoth = uiState.request.includeMovies && uiState.request.includeTvShows

                    SelectableChip(
                        text = "Movies",
                        icon = Icons.Default.PlayArrow,
                        selected = isMovies,
                        onClick = { 
                            viewModel.setMediaTypes(movies = true, tvShows = false)
                            viewModel.generateSelection(skipReveal = true)
                        },
                        modifier = Modifier.focusRequester(contentFocusRequester)
                    )
                    SelectableChip(
                        text = "TV Shows",
                        icon = Icons.AutoMirrored.Filled.List,
                        selected = isTvShows,
                        onClick = { 
                            viewModel.setMediaTypes(movies = false, tvShows = true)
                            viewModel.generateSelection(skipReveal = true)
                        }
                    )
                    SelectableChip(
                        text = "Both",
                        icon = CasinoDiceIcon,
                        selected = isBoth,
                        onClick = { 
                            viewModel.setMediaTypes(movies = true, tvShows = true)
                            viewModel.generateSelection(skipReveal = true)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Controls Row 2: Quantity
                Text(
                    text = "Number of picks",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val quantities = listOf(1, 3, 5, 10)
                    for (q in quantities) {
                        SelectableNumberChip(
                            number = q,
                            selected = uiState.selectedQuantity == q,
                            onClick = { 
                                viewModel.setQuantity(q)
                                viewModel.generateSelection(skipReveal = true)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Hero Section embedded directly
                HeroSection(
                    focusedMovieProvider = { focusedMovie },
                    modifier = Modifier.weight(1f) // Let it fill space to push carousel to bottom
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { 
                            val index = uiState.results.indexOf(focusedMovie)
                            if (index != -1) {
                                viewModel.replaceResult(index)
                            } else {
                                viewModel.generateSelection(skipReveal = true)
                            }
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color(0xFF00D2FF),
                            contentColor = Color.White,
                            focusedContentColor = Color(0xFF040B16)
                        ),
                        shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(imageVector = CasinoDiceIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick Another", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { /* Watchlist Stub */ },
                        enabled = false, // Disabled due to missing repository logic
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.05f),
                            focusedContainerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White.copy(alpha = 0.5f),
                            focusedContentColor = Color.White
                        ),
                        shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Watchlist")
                    }
                    
                    Button(
                        onClick = { focusedMovie?.let { onMovieClick(it) } },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.White,
                            contentColor = Color.White,
                            focusedContentColor = Color(0xFF040B16)
                        ),
                        shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("More Info")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Carousel
                Text(
                    text = "More Random Picks",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (uiState.status == RandomStatus.LOADING && uiState.results.isEmpty()) {
                    Text("Loading...", color = Color.White.copy(alpha = 0.5f))
                } else if (uiState.results.isEmpty()) {
                    Text("No results found.", color = Color.White.copy(alpha = 0.5f))
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(end = 48.dp)
                    ) {
                        itemsIndexed(uiState.results, key = { _, movie -> "${movie.mediaType.name}_${movie.movieId}" }) { _, movie ->
                            DetailMovieCard(
                                movie = movie,
                                onMovieFocused = { m -> focusedMovie = m },
                                onMovieClick = { m -> onMovieClick(m) },
                                focusedBorderColor = Color(0xFF00D2FF)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SelectableChip(
    text: String,
    icon: ImageVector? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = if (selected) Color(0xFF00D2FF) else Color.White,
            contentColor = if (selected) Color(0xFF00D2FF) else Color.White.copy(alpha = 0.7f),
            focusedContentColor = Color(0xFF040B16)
        ),
        shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(1.dp, if (selected) Color(0xFF00D2FF) else Color.White.copy(alpha = 0.3f))),
            focusedBorder = Border(BorderStroke(0.dp, Color.Transparent))
        ),
        modifier = modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
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
            containerColor = Color.Transparent,
            focusedContainerColor = if (selected) Color(0xFF00D2FF) else Color.White,
            contentColor = if (selected) Color(0xFF00D2FF) else Color.White.copy(alpha = 0.7f),
            focusedContentColor = Color(0xFF040B16)
        ),
        shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(1.dp, if (selected) Color(0xFF00D2FF) else Color.White.copy(alpha = 0.3f))),
            focusedBorder = Border(BorderStroke(0.dp, Color.Transparent))
        ),
        modifier = modifier.width(64.dp).height(40.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = number.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}
