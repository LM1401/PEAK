package com.example.peak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.peak.domain.model.Movie

/**
 * Shared Base Layout for PEAK Browse screens (Home, Movies, etc.)
 * Rebuilt as a clean Parallel HUD shell using Box-based layering.
 * Follows a deterministic overlay model where content and HUDs are independently managed.
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
        // LAYER 1 — BACKGROUND (zIndex 0-1)
        CinematicBackground(
            backdropUrl = focusedMovie?.backdropUrl,
            movieId = focusedMovie?.movieId,
            modifier = Modifier.fillMaxSize().zIndex(0f)
        )
        HomeGradientsOverlay(
            modifier = Modifier.fillMaxSize().zIndex(1f)
        )

        // LAYER 2 — CONTENT VIEWPORT (zIndex 2)
        // Pure Canvas: Receives full-screen constraints and manages own spacing via scroll.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .zIndex(2f)
        ) {
            rowsContent(Modifier.fillMaxSize())
        }

        // LAYER 3 — HERO HUD (zIndex 3)
        // Pure Overlay: Deterministic positioning, layout-agnostic.
        HeroSection(
            movie = focusedMovie,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 120.dp, top = 80.dp)
                .zIndex(3f)
                .focusProperties { canFocus = false }
        )

        // LAYER 4 — NAVIGATION HUD (zIndex 4)
        // Top-most interactive layer for global navigation.
        TopNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onSettingsClick = onSettingsClick,
            onSearchClick = onSearchClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .zIndex(4f)
                .focusRequester(navFocusRequester)
                .focusProperties {
                    down = contentFocusRequester
                }
        )

        // LAYER 5 — LOADING OVERLAY (zIndex 20)
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
