package com.example.peak.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
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
 * REFACTORED: Uses the deterministic TV Camera System instead of LazyColumn.
 * Rows are placed in a world stack and translated based on focus position.
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
    val navFocusRequester = remember { FocusRequester() }

    // TV CAMERA SYSTEM INITIALIZATION
    val cameraState = rememberTvCameraState()
    var isInitialised by remember { mutableStateOf(false) }

    // 1. VIEWPORT TRACKING
    val viewportModifier = Modifier.onGloballyPositioned { coords ->
        cameraState.viewportHeight = coords.size.height.toFloat()
    }

    // 2. FOCUS -> CAMERA SYNC
    LaunchedEffect(focusState?.movieId, rows) {
        val movieId = focusState?.movieId ?: return@LaunchedEffect
        val row = rows.find { it.movies.any { m -> m.movieId == movieId } }
        
        row?.let {
            if (!isInitialised) {
                cameraState.snapToRow(it.id)
                isInitialised = true
            } else {
                cameraState.scrollToRow(it.id)
            }
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
                .then(viewportModifier)
                .focusRequester(contentFocusRequester)
                .focusProperties {
                    up = navFocusRequester
                }
        ) {
            // THE WORLD (Translates based on Camera State)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvCameraWorld(cameraState)
            ) {
                // 1. GLOBAL ANCHOR: Only this spacer defines the start of content.
                // Adjusted to 220.dp for tighter Hero-to-Row alignment.
                Spacer(modifier = Modifier.height(220.dp))

                // 2. CONTENT RHYTHM: Inter-row spacing applied manually to avoid anchor inflation.
                rows.forEachIndexed { index, row ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    MovieRow(
                        row = row,
                        focusedMovieId = focusState?.movieId,
                        onMovieFocused = { id -> viewModel.onMovieFocused(id) },
                        onMovieSelected = viewModel::onMovieSelected,
                        onMovieClick = onMovieClick,
                        progressMap = if (row.id == "continue_watching") continueWatchingProgress else null,
                        focusManager = focusManager,
                        modifier = Modifier.onRowPositioned(row.id, cameraState)
                    )
                }

                if (!loading && rows.isEmpty()) {
                    EmptyHomePlaceholder()
                }

                // Bottom spacer for overshoot/safe-area
                Spacer(modifier = Modifier.height(200.dp))
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
