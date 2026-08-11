package com.example.peak.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.MovieRow
import com.example.peak.ui.focus.rememberFocusMemoryManager
import com.example.peak.ui.focus.rememberTvCameraState
import com.example.peak.ui.focus.tvCameraWorld
import com.example.peak.ui.focus.onRowPositioned
import com.example.peak.ui.image.ImageWarmingManager
import com.example.peak.ui.image.PeakImageLoader

/**
 * HomeScreen.
 * REFACTORED: Deterministic Slot Viewport.
 * Uses index-based camera translation to ensure "One Active + One Peek" UX
 * across all resolutions.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTabSelected: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val focusState by viewModel.focusState.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val continueWatchingProgress by viewModel.continueWatchingProgress.collectAsState()

    val context = LocalContext.current
    val imageLoader = remember { PeakImageLoader.getInstance(context) }
    val focusManager = rememberFocusMemoryManager()
    val contentFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        contentFocusRequester.requestFocus()
    }
    val navFocusRequester = remember { FocusRequester() }

    // TV CAMERA SYSTEM: Restored vertical slot responsibility
    val cameraState = rememberTvCameraState()

    // SYNC FOCUS -> CAMERA SLOT
    val focusRowIndex = remember(focusState?.rowId, rows) {
        val rowId = focusState?.rowId
        rows.indexOfFirst { it.id == rowId }
    }

    LaunchedEffect(focusRowIndex) {
        if (focusRowIndex != -1) {
            cameraState.scrollToRow(focusRowIndex)
        }
    }

    LaunchedEffect(rows) {
        if (rows.isNotEmpty()) {
            val moviesToWarm = rows.take(3).flatMap { it.movies }
            ImageWarmingManager.warm(context, imageLoader, moviesToWarm)
        }
    }

    HomeBaseLayout(
        selectedTab = "Home",
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        onSearchClick = onSearchClick,
        focusedMovie = focusState?.movie,
        showLoadingOverlay = loading && rows.isEmpty(),
        navFocusRequester = navFocusRequester,
        contentFocusRequester = contentFocusRequester
    ) { modifier ->
        // THE VIEWPORT
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 220.dp)
                .clipToBounds()
                .focusRequester(contentFocusRequester)
                .focusProperties {
                    up = navFocusRequester
                }
        ) {
            // THE WORLD: Stacks rows vertically and translates by Slot Index.
            // wrapContentHeight(unbounded = true) is CRITICAL for focus: it allows the 
            // focus system to see rows that are layout-positioned below the viewport.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(unbounded = true, align = Alignment.Top)
                    .tvCameraWorld(cameraState)
            ) {
                // CONTENT SLOTS
                rows.forEachIndexed { index, row ->
                    key(row.id) {
                        // SLOT SPACING: Exact 24dp for deterministic anchor math
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        MovieRow(
                            row = row,
                            focusedMovieId = focusState?.movieId,
                            onMovieFocused = { rowId, movieId -> viewModel.onMovieFocused(rowId, movieId) },
                            onMovieSelected = viewModel::onMovieSelected,
                            onMovieClick = onMovieClick,
                            progressMap = if (row.id == "continue_watching") continueWatchingProgress else null,
                            focusManager = focusManager,
                            modifier = Modifier.onRowPositioned(row.id, cameraState)
                        )
                    }
                }

                if (!loading && rows.isEmpty()) {
                    EmptyHomePlaceholder()
                }

                // Overshoot spacer to allow last row to be "Active Slot"
                Spacer(modifier = Modifier.height(600.dp))
            }
        }
    }
}

@Composable
fun EmptyHomePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(horizontal = 120.dp),
        contentAlignment = androidx.compose.ui.Alignment.CenterStart
    ) {
        androidx.compose.foundation.layout.Column {
            androidx.tv.material3.Text(
                text = "No content available right now.",
                style = androidx.tv.material3.MaterialTheme.typography.headlineSmall,
                color = androidx.compose.ui.graphics.Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            androidx.tv.material3.Text(
                text = "Try checking your internet connection or come back later.",
                style = androidx.tv.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.Gray
            )
        }
    }
}
