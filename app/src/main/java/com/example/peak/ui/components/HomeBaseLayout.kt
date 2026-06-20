package com.example.peak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.metadata.MovieMetadataSection

/**
 * Shared Base Layout for PEAK Browse screens (Home, Movies, etc.)
 * Ensures consistent metadata placement, background behavior and navigation.
 */
@Composable
fun HomeBaseLayout(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    focusedMovie: Movie?,
    rowsContent: @Composable (Modifier) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. BACKGROUND LAYER (Fixed)
        Box(modifier = Modifier.fillMaxSize()) {
            CinematicBackground(
                backdropUrl = focusedMovie?.backdropUrl,
                movieId = focusedMovie?.movieId,
                modifier = Modifier.fillMaxSize().zIndex(0f)
            )
            HomeGradientsOverlay(
                modifier = Modifier.fillMaxSize().zIndex(1f)
            )
        }

        // 2. CONTENT LAYER
        // We use a Box here to allow the LazyColumn to be full-screen
        // while Metadata and TopBar are overlaid.
        Box(modifier = Modifier.fillMaxSize()) {
            
            // THE ROWS (LazyColumn content)
            // modifier should be fillMaxSize to allow scrolling behind overlays
            rowsContent(Modifier.fillMaxSize())

            // TOP NAVIGATION (Fixed)
            Column(modifier = Modifier.fillMaxWidth().zIndex(5f)) {
                TopNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    onSettingsClick = onSettingsClick,
                    onSearchClick = onSearchClick
                )
                
                // HERO SECTION (Fixed Title)
                HeroSection(
                    movie = focusedMovie,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 120.dp)
                        .padding(top = 24.dp)
                )
            }

            // METADATA SECTION (Fixed Overlay at Bottom)
            // Independent of LazyColumn scroll state.
            MovieMetadataSection(
                movie = focusedMovie,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .zIndex(10f) // Ensure it's above the rows
            )
        }
    }
}
