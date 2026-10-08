package com.example.peak.ui.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.MovieCard
import com.example.peak.ui.screens.movies.components.MovieLandscapeRow
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.ui.focus.rememberFocusMemoryManager
import com.example.peak.ui.focus.rememberMovieRowFocusManager
import com.example.peak.ui.screens.home.HomeConstants
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.yield

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onSidebarItemSelected: (SidebarItemType) -> Unit = {},
    onItemClick: (Movie) -> Unit = {},
    onDiscoveryClick: (String, String) -> Unit = { _, _ -> },
    onLearnMoreClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val navFocusRequester = remember { FocusRequester() }
    val searchInputFocusRequester = remember { FocusRequester() }
    val learnMoreRequester = remember { FocusRequester() }
    val recommendedFirstRequester = remember { FocusRequester() }
    val resultsGridRequester = remember { FocusRequester() }
    val genreFocusRequesters = remember {
        SearchDiscoveryConfig.mainCategories.associate { it.id to FocusRequester() }
    }
    val focusMemoryManager = rememberFocusMemoryManager()
    val focusRegistry = rememberMovieRowFocusManager()

    val lifecycleOwner = LocalLifecycleOwner.current
    var restoreTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                restoreTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(restoreTrigger) {
        if (restoreTrigger > 0) {
            val rememberedKey = focusMemoryManager.getRememberedId("search_screen")
            val targetRequester = when {
                rememberedKey == "search_input" -> searchInputFocusRequester
                rememberedKey == "learn_more" -> learnMoreRequester
                rememberedKey == "search_recommended" -> recommendedFirstRequester
                rememberedKey == "results_grid" -> resultsGridRequester
                rememberedKey?.startsWith("genre_") == true -> {
                    val genreId = rememberedKey.removePrefix("genre_")
                    genreFocusRequesters[genreId]
                }
                else -> null
            } ?: searchInputFocusRequester

            try {
                targetRequester.requestFocus()
            } catch (e: Exception) {
                yield()
                try {
                    targetRequester.requestFocus()
                } catch (e2: Exception) {
                    try { searchInputFocusRequester.requestFocus() } catch (_: Exception) {}
                }
            }
        }
    }

    var isSearchFocused by remember { mutableStateOf(false) }

    val rememberedKey = focusMemoryManager.getRememberedId("search_screen")
    val dynamicContentFocusRequester = when {
        rememberedKey == "search_input" -> searchInputFocusRequester
        rememberedKey == "learn_more" -> learnMoreRequester
        rememberedKey == "search_recommended" -> recommendedFirstRequester
        rememberedKey == "results_grid" -> resultsGridRequester
        rememberedKey?.startsWith("genre_") == true -> {
            val genreId = rememberedKey.removePrefix("genre_")
            genreFocusRequesters[genreId]
        }
        else -> null
    } ?: searchInputFocusRequester

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.SEARCH,
        onSidebarItemSelected = onSidebarItemSelected,
        focusedMovieProvider = { null },
        navFocusRequester = navFocusRequester,
        contentFocusRequester = dynamicContentFocusRequester,
        heroHud = null
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = HomeConstants.HOME_CONTENT_START_PADDING,
                    top = 64.dp,
                    end = 48.dp
                )
        ) {
            // SEARCH HEADING
            Text(
                text = "FIND YOUR NEXT STORY",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SEARCH",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Light,
                letterSpacing = 8.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // SEARCH INPUT BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(64.dp)
                    .border(
                        width = if (isSearchFocused) 2.dp else 1.dp,
                        brush = Brush.horizontalGradient(
                            if (isSearchFocused) listOf(Color.Cyan, Color.Blue, Color.Red)
                            else listOf(Color.Gray.copy(alpha=0.5f), Color.DarkGray)
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChanged,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 18.sp
                    ),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchInputFocusRequester)
                        .onFocusChanged {
                            isSearchFocused = it.hasFocus
                            if (it.hasFocus) {
                                focusMemoryManager.saveFocus("search_screen", "search_input")
                            }
                        }
                        .focusProperties {
                            left = navFocusRequester
                            down = if (uiState.query.isBlank()) (genreFocusRequesters["horror"] ?: FocusRequester.Default) else resultsGridRequester
                        },
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchFocused) Color.White else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            if (uiState.query.isEmpty()) {
                                Text(
                                    text = "Search series, films, games...",
                                    color = Color.Gray,
                                    fontSize = 18.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.query.isBlank()) {
                // DISCOVERY MODE (IDLE)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TRY A NEW WAY TO SEARCH",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.titleSmall,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.Red, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("BETA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DISCOVERY BUTTON GRID (2x3)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val row1 = SearchDiscoveryConfig.mainCategories.take(3)
                    val row2 = SearchDiscoveryConfig.mainCategories.drop(3).take(3)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row1.forEachIndexed { index, cat ->
                            val catRequester = genreFocusRequesters[cat.id] ?: remember { FocusRequester() }
                            DiscoveryButton(
                                category = cat,
                                onClick = { onDiscoveryClick(cat.label, cat.tmdbGenreIds) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .focusRequester(catRequester)
                                    .onFocusChanged {
                                        if (it.hasFocus) {
                                            focusMemoryManager.saveFocus("search_screen", "genre_${cat.id}")
                                        }
                                    }
                                    .focusProperties {
                                        if (index == 0) left = navFocusRequester
                                    }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row2.forEachIndexed { index, cat ->
                            val catRequester = genreFocusRequesters[cat.id] ?: remember { FocusRequester() }
                            DiscoveryButton(
                                category = cat,
                                onClick = { onDiscoveryClick(cat.label, cat.tmdbGenreIds) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .focusRequester(catRequester)
                                    .onFocusChanged {
                                        if (it.hasFocus) {
                                            focusMemoryManager.saveFocus("search_screen", "genre_${cat.id}")
                                        }
                                    }
                                    .focusProperties {
                                        if (index == 0) left = navFocusRequester
                                        down = learnMoreRequester
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // LEARN MORE BUTTON
                Surface(
                    onClick = onLearnMoreClick,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(48.dp)
                        .focusRequester(learnMoreRequester)
                        .onFocusChanged {
                            if (it.hasFocus) {
                                focusMemoryManager.saveFocus("search_screen", "learn_more")
                            }
                        }
                        .focusProperties {
                            left = navFocusRequester
                            down = recommendedFirstRequester
                        },
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    border = ClickableSurfaceDefaults.border(
                        border = Border(BorderStroke(1.dp, Color.Gray.copy(alpha=0.5f))),
                        focusedBorder = Border(BorderStroke(2.dp, Color.White))
                    ),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Learn More >", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // RECOMMENDED ROW
                if (uiState.trendingResults.isNotEmpty()) {
                    MovieLandscapeRow(
                        rowId = "search_recommended",
                        title = "RECOMMENDED SERIES & FILMS",
                        movies = uiState.trendingResults,
                        onMovieFocused = { _, movie ->
                            focusMemoryManager.saveFocus("search_screen", "search_recommended")
                            viewModel.onItemFocused(movie)
                        },
                        onMovieSelected = onItemClick,
                        onMovieClick = onItemClick,
                        navFocusRequester = navFocusRequester,
                        getRequester = { _, index -> 
                            if (index == 0) recommendedFirstRequester else FocusRequester.Default
                        },
                        getPrevRowRequester = { learnMoreRequester },
                        getNextRowRequester = { null },
                        focusMemoryManager = focusMemoryManager,
                        isFocusedRow = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            } else {
                // ACTIVE SEARCH MODE (RESULTS)
                // Suggestion Chips
                if (uiState.suggestions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.suggestions) { suggestion ->
                            Surface(
                                onClick = { viewModel.onQueryChanged(suggestion) },
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = Color(0xFF1A1A1A),
                                    focusedContainerColor = Color.White
                                ),
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    text = suggestion,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Results Grid
                if (uiState.isLoading && uiState.results.isEmpty()) {
                    Text("Searching...", color = Color.Gray)
                } else if (uiState.results.isEmpty()) {
                    Text("No results found for \"${uiState.query}\"", color = Color.Gray)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(resultsGridRequester)
                            .onFocusChanged {
                                if (it.hasFocus) {
                                    focusMemoryManager.saveFocus("search_screen", "results_grid")
                                }
                            }
                            .focusProperties {
                                left = navFocusRequester
                                up = searchInputFocusRequester
                            }
                    ) {
                        items(uiState.results, key = { it.movieId }) { movie ->
                            Box(modifier = Modifier.aspectRatio(2f/3f)) {
                                MovieCard(
                                    movie = movie,
                                    onFocus = { viewModel.onItemFocused(it!!) },
                                    onClick = onItemClick,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DiscoveryButton(
    category: SearchDiscoveryConfig.DiscoveryCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 96.dp,
    horizontalPadding: Dp = 20.dp
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF0A101D),
            focusedContainerColor = Color(0xFF16203A)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))),
            focusedBorder = Border(BorderStroke(2.dp, Color.White.copy(alpha = 0.9f)))
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = Color.Black.copy(alpha = 0.5f),
                elevation = 16.dp
            )
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (category.iconRes != null) {
                Box(
                    modifier = Modifier.size(iconSize),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = category.iconRes),
                        contentDescription = category.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(iconSize)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = category.label.uppercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.75.sp,
                maxLines = 2,
                lineHeight = 18.sp,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
