package com.example.peak.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.example.peak.ui.image.ImagePreloader
import com.example.peak.ui.image.PeakImageLoader
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBackClick: () -> Unit = {},
    onItemClick: (SearchItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    
    val keyboardRows = listOf(
        listOf("A", "B", "C", "D", "E", "F"),
        listOf("G", "H", "I", "J", "K", "L"),
        listOf("M", "N", "O", "P", "Q", "R"),
        listOf("S", "T", "U", "V", "W", "X"),
        listOf("Y", "Z", "1", "2", "3", "4"),
        listOf("5", "6", "7", "8", "9", "0")
    )

    // Preloader Integration: Optimized for TV performance using snapshotFlow
    val displayItems = if (uiState.query.isEmpty()) uiState.trendingResults else uiState.results
    LaunchedEffect(displayItems) {
        snapshotFlow { gridState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { index ->
                if (displayItems.isNotEmpty()) {
                    val startIndex = index
                    val endIndex = (startIndex + 15).coerceAtMost(displayItems.size)
                    val urls = displayItems.subList(startIndex, endIndex)
                        .mapNotNull { it.posterUrl }
                    ImagePreloader.preload(context, urls)
                }
            }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // LEFT PANEL: Controls & Keyboard
        Column(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .padding(start = 48.dp, top = 24.dp, end = 24.dp)
        ) {
            Surface(
                onClick = onBackClick,
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Back to Browse", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = uiState.query.ifEmpty { "Search..." },
                    color = if (uiState.query.isEmpty()) Color.DarkGray else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                keyboardRows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { char ->
                            KeyItem(
                                char = char,
                                onClick = { viewModel.onQueryChanged(uiState.query + char) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    KeyItem(
                        char = "Space",
                        onClick = { viewModel.onQueryChanged(uiState.query + " ") },
                        modifier = Modifier.weight(2f)
                    )
                    KeyItem(
                        char = "Delete",
                        onClick = { 
                            if (uiState.query.isNotEmpty()) {
                                viewModel.onQueryChanged(uiState.query.dropLast(1))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Suggestions List
            val suggestions = uiState.suggestions

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { suggestion ->
                    Surface(
                        onClick = { viewModel.onQueryChanged(suggestion) },
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.Transparent,
                            focusedContainerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = suggestion,
                            color = Color.Gray,
                            fontSize = 16.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }

        // RIGHT PANEL: Content Grid
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 24.dp, end = 48.dp)
        ) {
            Text(
                text = if (uiState.query.isEmpty()) "Trending Now" else "Results for \"${uiState.query}\"",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading && displayItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading...", color = Color.Red)
                    }
                } else if (displayItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No items found", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 48.dp)
                    ) {
                        items(displayItems, key = { it.id }) { item ->
                            SearchGridItem(
                                item = item,
                                onClick = { onItemClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun KeyItem(
    char: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF1A1A1A),
            focusedContainerColor = Color.White
        ),
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = char,
                color = if (char.length > 1) Color.Gray else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchGridItem(
    item: SearchItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, Color.White),
                shape = RoundedCornerShape(4.dp)
            )
        ),
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // BACKUP: Placeholder background if image is loading or missing
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF222222)))

            // IMAGE: Using the existing PeakImageLoader system
            AsyncImage(
                model = PeakImageLoader.buildRequest(context, item.posterUrl),
                imageLoader = PeakImageLoader.getInstance(context),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // OVERLAY: Gradient for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold
                )
                item.year?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
