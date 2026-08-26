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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.peak.ui.screens.home.HomeConstants
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

import com.example.peak.ui.components.sidebar.SidebarItemType

/**
 * HomeScreen.
 * REFACTORED: Deterministic Slot Viewport.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSidebarItemSelected: (SidebarItemType) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    val focusState by viewModel.focusState.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val continueWatchingProgress by viewModel.continueWatchingProgress.collectAsState()

    val context = LocalContext.current
    val focusManager = rememberFocusMemoryManager()
    val contentFocusRequester = remember { FocusRequester() }
    val navFocusRequester = remember { FocusRequester() }

    // TV CAMERA SYSTEM: Restored vertical slot responsibility
    val cameraState = rememberTvCameraState()

    // FOCUS RESTORATION LOGIC
    val lifecycleOwner = LocalLifecycleOwner.current
    var isInitialFocusRequested by rememberSaveable { mutableStateOf(false) }
    var restorationTarget by remember { mutableStateOf<FocusState?>(null) }
    var startupFocusTarget by remember { mutableStateOf<FocusState?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isInitialFocusRequested) {
                    if (focusState != null) {
                        restorationTarget = focusState
                    }
                } else {
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

    // AUTO-FOLLOW FOCUS: If the user hasn't manually moved focus, follow the ViewModel's state.
    // This ensures focus moves to 'Continue Watching' if it arrives after 'Trending' during startup.
    var hasUserMovedFocus by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(focusState) {
        if (!hasUserMovedFocus && focusState != null) {
            // DO NOT set restorationTarget here; it triggers snapToRow which breaks bottom-row peek.
            // Instead, use a dedicated startup target for UI focus, allowing scrollToRow to handle the camera.
            startupFocusTarget = focusState
        }
    }

    LaunchedEffect(focusRowIndex) {
        if (focusRowIndex != -1 && restorationTarget == null) {
            cameraState.scrollToRow(focusRowIndex)
        }
    }

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.HOME,
        onSidebarItemSelected = onSidebarItemSelected,
        focusedMovie = focusState?.movie,
        showLoadingOverlay = loading && rows.isEmpty(),
        navFocusRequester = navFocusRequester,
        contentFocusRequester = contentFocusRequester
    ) { modifier ->
        // THE VIEWPORT
        Box(
            modifier = modifier
                .padding(top = HomeConstants.HOME_HERO_BOTTOM_ANCHOR)
                .width(LocalConfiguration.current.screenWidthDp.dp)
                .height(HomeConstants.HOME_VIEWPORT_HEIGHT)
                .clipToBounds()
                .focusRequester(contentFocusRequester)
                .focusProperties {
                    left = navFocusRequester
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(unbounded = true, align = Alignment.Top)
                    .tvCameraWorld(cameraState)
            ) {
                // CONTENT SLOTS
                rows.forEachIndexed { index, row ->
                    key(row.id) {
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(HomeConstants.HOME_ROW_SPACING))
                        }

                        MovieRow(
                            row = row,
                            focusedMovieId = focusState?.movieId,
                            isFocused = focusState?.rowId == row.id,
                            isNearViewport = if (focusRowIndex == -1) index <= 1 else kotlin.math.abs(index - focusRowIndex) <= 1,
                            onMovieFocused = { rowId, movieId, mediaType -> 
                                // Track manual movement to stop auto-following focus
                                if (focusState != null && (focusState?.rowId != rowId || focusState?.movieId != movieId)) {
                                    hasUserMovedFocus = true
                                    startupFocusTarget = null // Ensure no pending auto-focus
                                }
                                viewModel.onMovieFocused(rowId, movieId, mediaType) 
                            },
                            onMovieSelected = viewModel::onMovieSelected,
                            onMovieClick = onMovieClick,
                            progressMap = if (row.id == "continue_watching") continueWatchingProgress else null,
                            focusManager = focusManager,
                            restorationMovieId = (restorationTarget ?: startupFocusTarget)?.let { 
                                if (it.rowId == row.id) it.movieId else null 
                            },
                            restorationMediaType = (restorationTarget ?: startupFocusTarget)?.let { 
                                if (it.rowId == row.id) it.mediaType else null 
                            },
                            onRestorationComplete = { 
                                restorationTarget = null 
                                startupFocusTarget = null
                            },
                            modifier = Modifier.onRowPositioned(row.id, cameraState)
                        )
                    }
                }

                if (!loading && rows.isEmpty()) {
                    EmptyHomePlaceholder()
                }

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
