package com.example.peak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.metadata.MovieMetadataSection

/**
 * Shared Base Layout for PEAK Browse screens (Home, Movies, etc.)
 * Ensures consistent metadata placement, background behavior and navigation.
 * Transitions from a stacked Column to a Parallel HUD Architecture.
 */
@Composable
fun HomeBaseLayout(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    focusedMovie: Movie?,
    showLoadingOverlay: Boolean = false,
    navFocusRequester: FocusRequester = remember { FocusRequester() },
    contentFocusRequester: FocusRequester = remember { FocusRequester() },
    rowsContent: @Composable (Modifier) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. BACKGROUND LAYER (zIndex 0-1)
        CinematicBackground(
            backdropUrl = focusedMovie?.backdropUrl,
            movieId = focusedMovie?.movieId,
            modifier = Modifier.fillMaxSize().zIndex(0f)
        )
        HomeGradientsOverlay(
            modifier = Modifier.fillMaxSize().zIndex(1f)
        )

        // 2. CONTENT LAYER (zIndex 2)
        // Measured under identical constraints without sequential height reduction.
        // LazyColumn receives full-screen constraints and handles item visibility via viewport clipping.
        rowsContent(
            Modifier
                .fillMaxSize()
                .zIndex(2f)
        )

        // 3. HUD OVERLAYS (zIndex 3+)
        // Measured independently of the Content Layer to prevent layout shifts.
        
        // A) TOP HUD (Navigation & Hero)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .zIndex(3f)
                .focusRequester(navFocusRequester)
                .focusProperties {
                    down = contentFocusRequester
                }
        ) {
            TopNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onSettingsClick = onSettingsClick,
                onSearchClick = onSearchClick
            )

            HeroSection(
                movie = focusedMovie,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 120.dp)
                    .padding(top = 20.dp, bottom = 20.dp)
                    .focusProperties { canFocus = false } // Neutralize HeroSection interference
            )
        }

        // B) METADATA HUD (Floating Overlay)
        MovieMetadataSection(
            movie = focusedMovie,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(4f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        // 4. LOADING OVERLAY
        if (showLoadingOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .background(Color.Black),
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
