package com.example.peak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.metadata.MovieMetadataSection

/**
 * Shared Base Layout for PEAK Browse screens (Home, Movies, etc.)
 * Ensures consistent metadata placement, background behavior and navigation.
 * THIS IS THE CANONICAL BROWSE LAYOUT OWNER.
 */
@Composable
fun HomeBaseLayout(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    focusedMovie: Movie?,
    showLoadingOverlay: Boolean = false,
    rowsContent: @Composable (Modifier) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. BACKGROUND LAYER (SINGLE INSTANCE)
        CinematicBackground(
            backdropUrl = focusedMovie?.backdropUrl,
            movieId = focusedMovie?.movieId,
            modifier = Modifier.fillMaxSize().zIndex(0f)
        )
        HomeGradientsOverlay(
            modifier = Modifier.fillMaxSize().zIndex(1f)
        )

        // 2. CONTENT LAYER (FOCUS SYSTEM)
        Column(modifier = Modifier.fillMaxSize()) {
            // A) TopNavigationBar - Must allow exit DOWN
            TopNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onSettingsClick = onSettingsClick,
                onSearchClick = onSearchClick
            )

            // B) HERO SECTION (SINGLE INSTANCE - FIXED)
            // Independent of scroll, but part of interaction Column
            HeroSection(
                movie = focusedMovie,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 120.dp)
                    .padding(top = 20.dp, bottom = 20.dp)
            )

            // C) MAIN CONTENT AREA (SCROLLABLE)
            // Rows are passed as content and weighted to fill space
            Box(modifier = Modifier.weight(1f)) {
                rowsContent(Modifier.fillMaxSize())
            }
            
            // D) METADATA LAYER (SINGLE INSTANCE - FIXED FOOTER)
            // Positioned at the bottom of the Column to ensure NO OVERLAP with rows
            MovieMetadataSection(
                movie = focusedMovie,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp) // SYNCED FIXED HEIGHT
                    .background(Color.Black.copy(alpha = 0.4f)) // Subtle scrim
            )
        }

        // 3. LOADING OVERLAY (SINGLE INSTANCE)
        if (showLoadingOverlay) {
            Box(
                modifier = Modifier.fillMaxSize().zIndex(20f).background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading PEAK...",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}
