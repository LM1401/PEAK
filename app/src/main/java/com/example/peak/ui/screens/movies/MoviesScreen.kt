package com.example.peak.ui.screens.movies

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
 * Movies Screen. 
 * REFACTORED: Deterministic Slot Viewport.
 */
@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel,
    onTabSelected: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val focusState by viewModel.focusState.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val loading by viewModel.loading.collectAsState()

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
        selectedTab = "Films",
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
                            onMovieFocused = { rowId, movieId ->
                                row.movies.find { it.movieId == movieId }?.let { movie ->
                                    viewModel.onMovieFocused(rowId, movie)
                                }
                            },
                            onMovieSelected = viewModel::onMovieSelected,
                            onMovieClick = onMovieClick,
                            focusManager = focusManager,
                            modifier = Modifier.onRowPositioned(row.id, cameraState)
                        )
                    }
                }

                if (!loading && rows.isEmpty()) {
                    EmptyMoviesPlaceholder()
                }

                Spacer(modifier = Modifier.height(600.dp))
            }
        }
    }
}

@Composable
fun EmptyMoviesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(horizontal = 120.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = "No movies found.",
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
