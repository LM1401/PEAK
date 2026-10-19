package com.example.peak.ui.screens.random

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.sidebar.CasinoDiceIcon
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.ui.screens.home.HomeConstants
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RandomConfigScreen(
    viewModel: RandomViewModel,
    onSidebarItemSelected: (SidebarItemType) -> Unit,
    onNavigateToResults: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val navFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    
    var isGenerating by remember { mutableStateOf(false) }

    // When status hits RESULTS, navigate if we initiated a generation
    LaunchedEffect(uiState.status) {
        if (isGenerating && uiState.status == RandomStatus.RESULTS) {
            isGenerating = false
            onNavigateToResults()
        } else if (isGenerating && uiState.status == RandomStatus.ERROR) {
            isGenerating = false
            // Handle error state if needed
        }
    }

    // Give focus to the first chip
    LaunchedEffect(Unit) {
        try {
            contentFocusRequester.requestFocus()
        } catch (e: Exception) {}
    }

    // Determine loading state for the overlay
    val showLoadingOverlay = (uiState.status == RandomStatus.LOADING || uiState.status == RandomStatus.REVEALING)

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.RANDOM,
        onSidebarItemSelected = onSidebarItemSelected,
        focusedMovieProvider = { null }, // No hero background movie for config screen
        navFocusRequester = navFocusRequester,
        contentFocusRequester = contentFocusRequester,
        heroHud = null // No hero HUD on config screen
    ) { modifier ->
        
        AnimatedContent(
            targetState = showLoadingOverlay,
            transitionSpec = {
                fadeIn(tween(350)) togetherWith fadeOut(tween(350))
            },
            label = "RandomConfigLoadingCrossfade",
            modifier = modifier.fillMaxSize()
        ) { isLoading ->
            if (isLoading) {
                // Cinematic Loading Experience
                Box(
                    modifier = Modifier.fillMaxSize().padding(start = HomeConstants.HOME_CONTENT_START_PADDING),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "R A N D O M",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Finding\nyour picks...",
                            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Searching for the best movies and TV shows\nbased on your preferences.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        // Simple progress bar mock
                        Box(
                            modifier = Modifier
                                .width(300.dp)
                                .height(4.dp)
                                .background(Color.White.copy(alpha = 0.1f), MaterialTheme.shapes.small)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(4.dp)
                                    .background(Color(0xFF00D2FF), MaterialTheme.shapes.small)
                            )
                        }
                    }
                }
            } else {
                // Config Screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = HomeConstants.HOME_CONTENT_START_PADDING,
                            top = 28.dp,
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
                        Text(
                            text = "R A N D O M",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Column {
                            Text(
                                text = "Find something",
                                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Row {
                                Text(
                                    text = "amazing ",
                                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "to watch",
                                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF00D2FF)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Get a random selection of movies and TV shows\nbased on your preferences.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Controls Row 1: Content Type
                        Text(
                            text = "WHAT WOULD YOU LIKE?",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val isMovies = uiState.request.includeMovies && !uiState.request.includeTvShows
                            val isTvShows = !uiState.request.includeMovies && uiState.request.includeTvShows
                            val isBoth = uiState.request.includeMovies && uiState.request.includeTvShows

                            ConfigSelectableChip(
                                text = "Movies",
                                icon = Icons.Default.PlayArrow,
                                selected = isMovies,
                                onClick = { 
                                    viewModel.setMediaTypes(movies = true, tvShows = false)
                                },
                                modifier = Modifier.focusRequester(contentFocusRequester)
                            )
                            ConfigSelectableChip(
                                text = "TV Shows",
                                icon = Icons.AutoMirrored.Filled.List,
                                selected = isTvShows,
                                onClick = { 
                                    viewModel.setMediaTypes(movies = false, tvShows = true)
                                }
                            )
                            ConfigSelectableChip(
                                text = "Both",
                                icon = CasinoDiceIcon,
                                selected = isBoth,
                                onClick = { 
                                    viewModel.setMediaTypes(movies = true, tvShows = true)
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Controls Row 2: Quantity
                        Text(
                            text = "NUMBER OF PICKS",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val quantities = listOf(1, 3, 5, 10)
                            for (q in quantities) {
                                ConfigSelectableNumberChip(
                                    number = q,
                                    selected = uiState.selectedQuantity == q,
                                    onClick = { 
                                        viewModel.setQuantity(q)
                                    }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { 
                                isGenerating = true
                                viewModel.generateSelection(skipReveal = false)
                            },
                            modifier = Modifier.width(420.dp).height(56.dp),
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0xFF00D2FF),
                                focusedContainerColor = Color.White,
                                contentColor = Color(0xFF040B16),
                                focusedContentColor = Color(0xFF040B16)
                            ),
                            shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
                            contentPadding = PaddingValues(horizontal = 32.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Icon(imageVector = CasinoDiceIcon, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Generate Picks", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ConfigSelectableChip(
    text: String,
    icon: ImageVector? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (selected) Color.White else Color.Transparent,
            focusedContainerColor = Color.White,
            contentColor = if (selected) Color(0xFF040B16) else Color.White.copy(alpha = 0.7f),
            focusedContentColor = Color(0xFF040B16)
        ),
        shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(1.dp, if (selected) Color.Transparent else Color.White.copy(alpha = 0.3f))),
            focusedBorder = Border(BorderStroke(0.dp, Color.Transparent))
        ),
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(text = text, style = MaterialTheme.typography.titleSmall)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ConfigSelectableNumberChip(
    number: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (selected) Color(0xFF00D2FF) else Color.Transparent,
            focusedContainerColor = Color.White,
            contentColor = if (selected) Color(0xFF040B16) else Color.White.copy(alpha = 0.7f),
            focusedContentColor = Color(0xFF040B16)
        ),
        shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.extraLarge), // Rounder shape for numbers
        border = ButtonDefaults.border(
            border = Border(BorderStroke(1.dp, if (selected) Color.Transparent else Color.White.copy(alpha = 0.3f))),
            focusedBorder = Border(BorderStroke(0.dp, Color.Transparent))
        ),
        modifier = modifier.width(80.dp).height(48.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = number.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
