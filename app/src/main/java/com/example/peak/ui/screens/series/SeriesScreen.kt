package com.example.peak.ui.screens.series

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.peak.domain.model.FocusState
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.MovieRow
import com.example.peak.ui.screens.home.HomeConstants
import com.example.peak.ui.focus.rememberFocusMemoryManager
import com.example.peak.ui.focus.rememberTvCameraState
import com.example.peak.ui.focus.tvCameraWorld
import com.example.peak.ui.focus.onRowPositioned
import com.example.peak.ui.image.ImageWarmingManager
import com.example.peak.ui.image.PeakImageLoader

import com.example.peak.ui.components.sidebar.SidebarItemType

/**
 * Series Screen. 
 * REFACTORED: Deterministic Slot Viewport.
 */
@Composable
fun SeriesScreen(
    viewModel: SeriesViewModel,
    onSidebarItemSelected: (SidebarItemType) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    val focusState by viewModel.focusState.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val loading by viewModel.loading.collectAsState()

    val context = LocalContext.current
    val focusManager = rememberFocusMemoryManager()
    val focusRegistry = com.example.peak.ui.focus.rememberMovieRowFocusManager()
    val contentFocusRequester = remember { FocusRequester() }
    val navFocusRequester = remember { FocusRequester() }

    // TV CAMERA SYSTEM: Restored vertical slot responsibility
    val cameraState = rememberTvCameraState()

    // FOCUS RESTORATION LOGIC
    val lifecycleOwner = LocalLifecycleOwner.current
    var isInitialFocusRequested by rememberSaveable { mutableStateOf(false) }
    var restorationTarget by remember { mutableStateOf<FocusState?>(null) }

    val onVerticalMove: (String, Int) -> Unit = { targetRowId, targetIndex ->
        val targetRow = rows.find { it.id == targetRowId }
        val targetMovie = targetRow?.movies?.getOrNull(targetIndex) ?: targetRow?.movies?.lastOrNull()
        if (targetMovie != null) {
            restorationTarget = FocusState(targetRowId, targetMovie.movieId, targetMovie.mediaType, targetMovie)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isInitialFocusRequested) {
                    // Return from Detail
                    if (focusState != null) {
                        restorationTarget = focusState
                    }
                } else {
                    // Initial entry
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
        // Only scroll if NOT currently restoring
        if (focusRowIndex != -1 && restorationTarget == null) {
            cameraState.scrollToRow(focusRowIndex)
        }
    }

    // REMOVED: screen-level warming sweep

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.TV,
        onSidebarItemSelected = onSidebarItemSelected,
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
                    left = navFocusRequester
                }
        ) {
            // THE WORLD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(unbounded = true, align = Alignment.Top)
                    .tvCameraWorld(cameraState)
            ) {
                rows.forEachIndexed { index, row ->
                    key(row.id) {
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        MovieRow(
                            row = row,
                            focusedMovieId = focusState?.movieId,
                            isFocused = focusState?.rowId == row.id,
                            isNearViewport = if (focusRowIndex == -1) index <= 1 else kotlin.math.abs(index - focusRowIndex) <= 1,
                            onMovieFocused = { rowId, movieId, mediaType ->
                                row.movies.find { it.movieId == movieId && it.mediaType == mediaType }?.let { movie ->
                                    viewModel.onMovieFocused(rowId, movie)
                                }
                            },
                            onMovieSelected = viewModel::onMovieSelected,
                            onMovieClick = onMovieClick,
                            focusManager = focusManager,
                            restorationMovieId = if (restorationTarget?.rowId == row.id) restorationTarget?.movieId else null,
                            restorationMediaType = if (restorationTarget?.rowId == row.id) restorationTarget?.mediaType else null,
                            onRestorationComplete = { restorationTarget = null },
                            onVerticalMove = onVerticalMove,
                            prevRowId = rows.getOrNull(index - 1)?.id,
                            prevRowSize = rows.getOrNull(index - 1)?.movies?.size ?: 0,
                            nextRowId = rows.getOrNull(index + 1)?.id,
                            nextRowSize = rows.getOrNull(index + 1)?.movies?.size ?: 0,
                            focusRegistry = focusRegistry,
                            navFocusRequester = navFocusRequester,
                            modifier = Modifier.onRowPositioned(row.id, cameraState)
                        )
                    }
                }

                if (!loading && rows.isEmpty()) {
                    EmptySeriesPlaceholder()
                }

                Spacer(modifier = Modifier.height(600.dp))
            }
        }
    }
}

@Composable
fun EmptySeriesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(horizontal = HomeConstants.HOME_CONTENT_START_PADDING),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = "No series found.",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try again later or check back for new additions.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
