package com.example.peak.ui.screens.watchlist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Watchlist
import com.example.peak.ui.components.CinematicBackground
import com.example.peak.ui.components.CinematicTitleView
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.MovieCard
import com.example.peak.ui.components.sidebar.CasinoDiceIcon
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.ui.screens.home.HomeConstants
import com.example.peak.ui.screens.watchlist.components.CreateListDialog
import com.example.peak.ui.screens.watchlist.components.EditListDialog
import com.example.peak.ui.screens.watchlist.components.RandomPickResultDialog

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onSidebarItemSelected: (SidebarItemType) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onPlayClick: (Movie) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val navFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }

    val currentHeroMovie = uiState.focusedMovie 
        ?: uiState.watchlists.firstOrNull { it.items.isNotEmpty() }?.items?.firstOrNull()

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.MY_LIST,
        onSidebarItemSelected = onSidebarItemSelected,
        focusedMovieProvider = { currentHeroMovie },
        showLoadingOverlay = uiState.isLoading,
        navFocusRequester = navFocusRequester,
        contentFocusRequester = contentFocusRequester,
        heroHud = null // We render our custom Watchlist Header + Hero in the scrollable layout
    ) { modifier ->

        Box(modifier = modifier.fillMaxSize()) {
            val mainScrollState = rememberLazyListState()

            LazyColumn(
                state = mainScrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = HomeConstants.HOME_CONTENT_START_PADDING),
                contentPadding = PaddingValues(top = 20.dp, bottom = 60.dp, end = 32.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // 1. HEADER & HERO HUD SLOT
                item(key = "watchlist_header_hero") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        // A. HEADER TITLE ROW
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "My Lists",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Your films. Your way.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }

                            // Header Actions
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                WatchlistHeaderButton(
                                    text = "Random Pick",
                                    icon = CasinoDiceIcon,
                                    onClick = { viewModel.randomPickAll() },
                                    focusRequester = contentFocusRequester,
                                    modifier = Modifier.focusProperties {
                                        left = navFocusRequester
                                    }
                                )

                                WatchlistHeaderButton(
                                    text = "Create List",
                                    icon = Icons.Default.Add,
                                    onClick = { viewModel.showCreateListDialog(true) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // B. CINEMATIC HERO SECTION
                        WatchlistHeroSection(
                            movie = currentHeroMovie,
                            onWatchTrailer = { movie -> movie?.let { onPlayClick(it) } },
                            onViewDetails = { movie -> movie?.let { onMovieClick(it) } }
                        )
                    }
                }

                // 2. WATCHLIST ROWS
                itemsIndexed(
                    items = uiState.watchlists,
                    key = { _, list -> "watchlist_row_${list.id}" }
                ) { index, watchlist ->
                    WatchlistRowSlot(
                        watchlist = watchlist,
                        isFirstRow = index == 0,
                        isSecondRow = index == 1,
                        isThirdRow = index == 2,
                        onMovieFocused = { movie -> viewModel.onMovieFocused(movie) },
                        onMovieClick = onMovieClick,
                        onRandomPickList = { listId, listName -> viewModel.randomPickFromList(listId, listName) },
                        onEditList = { listToEdit -> viewModel.showEditListDialog(listToEdit) }
                    )
                }

                if (!uiState.isLoading && uiState.watchlists.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "No lists found. Click 'Create List' to get started!",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // DIALOG OVERLAYS
            if (uiState.showCreateListDialog) {
                CreateListDialog(
                    onConfirm = { name -> viewModel.createList(name) },
                    onDismiss = { viewModel.showCreateListDialog(false) },
                    errorMessage = uiState.error
                )
            }

            uiState.listToEdit?.let { listToEdit ->
                EditListDialog(
                    watchlist = listToEdit,
                    onRename = { newName -> viewModel.renameList(listToEdit.id, newName) },
                    onDelete = { viewModel.deleteList(listToEdit.id) },
                    onDismiss = { viewModel.showEditListDialog(null) },
                    errorMessage = uiState.error
                )
            }

            uiState.randomPickResult?.let { pickedMovie ->
                RandomPickResultDialog(
                    movie = pickedMovie,
                    listName = uiState.randomPickListName ?: "Your Lists",
                    onViewDetails = { movie ->
                        viewModel.clearRandomPickResult()
                        onMovieClick(movie)
                    },
                    onPickAgain = {
                        if (uiState.randomPickListName == "All Lists" || uiState.randomPickListName == null) {
                            viewModel.randomPickAll()
                        } else {
                            val listId = uiState.watchlists.find { it.name == uiState.randomPickListName }?.id
                            if (listId != null) {
                                viewModel.randomPickFromList(listId, uiState.randomPickListName!!)
                            } else {
                                viewModel.randomPickAll()
                            }
                        }
                    },
                    onDismiss = { viewModel.clearRandomPickResult() }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WatchlistHeaderButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(38.dp)
            .focusRequester(focusRequester),
        colors = ButtonDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.12f),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color(0xFF040B16)
        ),
        shape = ButtonDefaults.shape(RoundedCornerShape(19.dp)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WatchlistHeroSection(
    movie: Movie?,
    onWatchTrailer: (Movie?) -> Unit,
    onViewDetails: (Movie?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        contentAlignment = Alignment.TopStart
    ) {
        if (movie != null) {
            Column(
                modifier = Modifier.width(620.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // Title artwork or fallback
                CinematicTitleView(
                    title = movie.name,
                    titleLogoUrl = movie.titleLogoUrl,
                    isEnriched = movie.isEnriched,
                    maxHeight = 64.dp,
                    maxWidth = 500.dp,
                    alignment = Alignment.CenterStart
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (movie.year.isNotBlank()) {
                        Text(
                            text = movie.year,
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (movie.duration.isNotBlank()) {
                        Text(text = "•", color = Color.White.copy(alpha = 0.3f))
                        Text(
                            text = movie.duration,
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (movie.genres.isNotBlank()) {
                        Text(text = "•", color = Color.White.copy(alpha = 0.3f))
                        Text(
                            text = movie.genres.split(",").take(2).joinToString(", "),
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Synopsis
                Text(
                    text = movie.description,
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hero Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onWatchTrailer(movie) },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xFF00D2FF),
                            focusedContainerColor = Color.White,
                            contentColor = Color(0xFF040B16),
                            focusedContentColor = Color(0xFF040B16)
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(20.dp)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Watch Trailer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = { onViewDetails(movie) },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = Color.White,
                            contentColor = Color.White,
                            focusedContentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(20.dp)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Details", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WatchlistRowSlot(
    watchlist: Watchlist,
    isFirstRow: Boolean,
    isSecondRow: Boolean,
    isThirdRow: Boolean,
    onMovieFocused: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onRandomPickList: (String, String) -> Unit,
    onEditList: (Watchlist) -> Unit
) {
    val rowLazyListState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // ROW HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = com.example.peak.ui.components.sidebar.BookmarkIcon,
                    contentDescription = null,
                    tint = Color(0xFF00D2FF),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = watchlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "${watchlist.titleCount} titles",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            // List Management Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                if (watchlist.items.isNotEmpty()) {
                    IconButton(
                        onClick = { onRandomPickList(watchlist.id, watchlist.name) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = CasinoDiceIcon,
                            contentDescription = "Random pick from this list",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onEditList(watchlist) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit list",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // CRITICAL LAYOUT SIZING REQUIREMENT:
        // For the 3rd row (e.g. Horror Night), if we are in initial viewport rendering,
        // we display the title row header, but do NOT show poster cards until user scrolls down!
        // When user scrolls down, the poster cards appear cleanly.
        if (watchlist.items.isNotEmpty()) {
            LazyRow(
                state = rowLazyListState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(end = 64.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(
                    items = watchlist.items,
                    key = { idx, movie -> "${watchlist.id}_${movie.mediaType.name}_${movie.movieId}_$idx" }
                ) { index, movie ->
                    Column(
                        modifier = Modifier
                            .width(145.dp)
                            .onFocusChanged { state ->
                                if (state.isFocused) {
                                    onMovieFocused(movie)
                                }
                            }
                    ) {
                        MovieCard(
                            movie = movie,
                            onFocus = { onMovieFocused(movie) },
                            onClick = { onMovieClick(movie) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This list is empty. Add titles from detail pages!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}
