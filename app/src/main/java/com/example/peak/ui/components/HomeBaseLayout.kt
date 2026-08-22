package com.example.peak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
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
import com.example.peak.ui.components.sidebar.Sidebar
import com.example.peak.ui.components.sidebar.SidebarItemType

/**
 * Shared Base Layout for PEAK Browse screens (Home, Movies, etc.)
 * Updated for the Stage 1 Redesign: Sidebar Navigation and Navy Palette.
 */
@Composable
fun HomeBaseLayout(
    selectedSidebarItem: SidebarItemType,
    onSidebarItemSelected: (SidebarItemType) -> Unit,
    focusedMovie: Movie?,
    showLoadingOverlay: Boolean = false,
    navFocusRequester: FocusRequester = remember { FocusRequester() },
    contentFocusRequester: FocusRequester = remember { FocusRequester() },
    rowsContent: @Composable (Modifier) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040B16)) // Deep Navy Base
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f)
        ) {
            rowsContent(Modifier.fillMaxSize())
        }

        // LAYER 3 — HERO HUD (zIndex 3)
        HeroSection(
            movie = focusedMovie,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 140.dp, top = 16.dp)
                .zIndex(3f)
                .focusProperties { canFocus = false }
        )

        // LAYER 4 — SIDEBAR NAVIGATION HUD (zIndex 4)
        // Overlay Sidebar: Does not push content, transparent/gradient background.
        Sidebar(
            selectedItem = selectedSidebarItem,
            onItemSelected = onSidebarItemSelected,
            onMoveRight = { contentFocusRequester.requestFocus() },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .zIndex(4f)
                .focusRequester(navFocusRequester)
                .focusProperties {
                    right = contentFocusRequester
                }
        )

        // LAYER 5 — LOADING OVERLAY (zIndex 20)
        if (showLoadingOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .background(Color(0xFF040B16)),
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
