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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.peak.domain.model.FocusState
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
    val navFocusRequester = remember { FocusRequester() }

    // TV CAMERA SYSTEM: Restored vertical slot responsibility
    val cameraState = rememberTvCameraState()

    // FOCUS RESTORATION LOGIC
    val lifecycleOwner = LocalLifecycleOwner.current
    var isInitialFocusRequested by rememberSaveable { mutableStateOf(false) }
    var restorationTarget by remember { mutableStateOf<FocusState?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isInitialFocusRequested) {
                    // Explicit restoration case: We've already been here, and we're coming back
                    if (focusState != null) {
                        restorationTarget = focusState
                    }
                } else {
                    // Fresh entry case: First time hitting the screen
                    contentFocusRequester.requestFocus()
                    isInitialFocusRequested = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // SYNC RESTORATION -> CAMERA SNAP
    LaunchedEffect(restorationTarget, rows) {
        restorationTarget?.let { target ->
            val index = rows.indexOfFirst { it.id == target.rowId }
            if (index != -1) {
                cameraState.snapToRow(index)
            }
        }
    }

    // SYNC FOCUS -> CAMERA SLOT
    val focusRowIndex = remember(focusState?.rowId, rows) {
        val rowId = focusState?.rowId
        rows.indexOfFirst { it.id == rowId }
    }

    LaunchedEffect(focusRowIndex) {
        // Only scroll if NOT currently restoring (restoration uses snapToRow)
        if (focusRowIndex != -1 && restorationTarget == null) {
            cameraState.scrollToRow(focusRowIndex)
        }
    }

    LaunchedEffect(rows) {
        if (rows.isNotEmpty()) {
            val moviesToWarm = rows.take(3).flatMap { it.movies }
            // STARTUP: Warm posters only, low priority
            ImageWarmingManager.warm(context, imageLoader, moviesToWarm, warmBackdrops = false)
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
                            onMovieFocused = { rowId, movieId, mediaType -> viewModel.onMovieFocused(rowId, movieId, mediaType) },
                            onMovieSelected = viewModel::onMovieSelected,
                            onMovieClick = onMovieClick,
                            progressMap = if (row.id == "continue_watching") continueWatchingProgress else null,
                            focusManager = focusManager,
                            restorationMovieId = if (restorationTarget?.rowId == row.id) restorationTarget?.movieId else null,
                            restorationMediaType = if (restorationTarget?.rowId == row.id) restorationTarget?.mediaType else null,
                            onRestorationComplete = { restorationTarget = null },
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
