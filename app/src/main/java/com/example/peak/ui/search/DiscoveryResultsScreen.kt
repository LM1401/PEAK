package com.example.peak.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.MovieCard
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.ui.screens.home.HomeConstants

@Composable
fun DiscoveryResultsScreen(
    viewModel: DiscoveryResultsViewModel,
    onSidebarItemSelected: (SidebarItemType) -> Unit,
    onItemClick: (Movie) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val navFocusRequester = remember { FocusRequester() }
    val gridFocusRequester = remember { FocusRequester() }

    var isInitialFocusRequested by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading, uiState.results) {
        if (!uiState.isLoading && uiState.results.isNotEmpty() && !isInitialFocusRequested) {
            try {
                gridFocusRequester.requestFocus()
                isInitialFocusRequested = true
            } catch (e: Exception) {}
        }
    }

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.SEARCH,
        onSidebarItemSelected = onSidebarItemSelected,
        focusedMovieProvider = { null },
        navFocusRequester = navFocusRequester,
        contentFocusRequester = gridFocusRequester,
        heroHud = null
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = HomeConstants.HOME_CONTENT_START_PADDING,
                    top = 64.dp,
                    end = 48.dp
                )
        ) {
            Text(
                text = uiState.title.uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Light,
                letterSpacing = 8.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (uiState.isLoading && uiState.results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading...", color = Color.White)
                }
            } else if (uiState.results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found.", color = Color.White)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(gridFocusRequester)
                        .focusProperties {
                            left = navFocusRequester
                        }
                ) {
                    items(uiState.results, key = { it.movieId }) { movie ->
                        Box(modifier = Modifier.aspectRatio(2f/3f)) {
                            MovieCard(
                                movie = movie,
                                onFocus = { viewModel.onItemFocused(it!!) },
                                onClick = onItemClick,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
