package com.example.peak.ui.screens.stream_selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.peak.domain.model.AddonStream
import com.example.peak.domain.model.AddonStreamProviderResult
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie

/**
 * Stream Selection Screen — PEAK Android TV Play Flow Integration.
 * Displays stream options retrieved from enabled Stremio add-ons.
 * Selecting a stream records state without initiating playback.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamSelectionScreen(
    mediaId: String,
    mediaType: MediaType,
    season: Int? = null,
    episode: Int? = null,
    viewModel: StreamSelectionViewModel,
    onStreamSelected: (AddonStream) -> Unit = {},
    onNavigateToAddons: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val initialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(mediaId, mediaType, season, episode) {
        viewModel.loadStreams(mediaId, mediaType, season, episode)
    }

    LaunchedEffect(uiState.isLoading, uiState.hasStreams, uiState.noAddonsConfigured, uiState.isTvWithoutEpisode) {
        if (!uiState.isLoading) {
            initialFocusRequester.requestFocus()
        }
    }

    val movie = uiState.movie
    val imageLoader = remember { com.example.peak.ui.image.PeakImageLoader.getInstance(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040B16))
    ) {
        // 1. FULL-BLEED BACKDROP
        if (movie != null && movie.backdropUrl.isNotBlank()) {
            val cacheKey = "${movie.mediaType.name}_${movie.movieId}_backdrop"
            AsyncImage(
                model = remember(movie.movieId, movie.mediaType, movie.backdropUrl) {
                    ImageRequest.Builder(context)
                        .data(movie.backdropUrl)
                        .memoryCacheKey(cacheKey)
                        .diskCacheKey(cacheKey)
                        .crossfade(false)
                        .allowHardware(true)
                        .build()
                },
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. GRADIENT OVERLAYS
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF040B16).copy(alpha = 0.98f),
                            Color(0xFF040B16).copy(alpha = 0.85f),
                            Color(0xFF040B16).copy(alpha = 0.40f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 900f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.40f),
                            Color.Transparent,
                            Color(0xFF040B16).copy(alpha = 0.90f),
                            Color(0xFF040B16)
                        )
                    )
                )
        )

        // 3. MAIN CONTENT LAYOUT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 80.dp, top = 28.dp, end = 48.dp, bottom = 28.dp)
        ) {
            // HEADER
            Text(
                text = (movie?.name ?: "STREAM SELECTION").uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AVAILABLE STREAMS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF22C55E), // PEAK Green accent
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontSize = 12.sp
                )

                if (movie != null && movie.year.isNotBlank()) {
                    Text(
                        text = " • ${movie.year}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                if (uiState.totalStreamCount > 0) {
                    Text(
                        text = " • ${uiState.totalStreamCount} sources found",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // STATE DISPLAY
            when {
                // LOADING STATE
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                text = "Searching enabled add-ons for available streams...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Querying providers (Torrentio, Cinemeta, etc.)...",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // TV SERIES WITHOUT EPISODE SELECTION
                uiState.isTvWithoutEpisode -> {
                    StreamInfoCard(
                        title = "Episode Selection Required",
                        description = "PEAK requires an explicit season and episode selection to retrieve TV series streams. Episode picker will be introduced in a future update.",
                        actionText = "Back to Details",
                        actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        focusRequester = initialFocusRequester,
                        onActionClick = onBackClick
                    )
                }

                // NO ADD-ONS CONFIGURED
                uiState.noAddonsConfigured -> {
                    StreamInfoCard(
                        title = "No Stream Add-ons Enabled",
                        description = "To discover streaming sources, install or enable compatible Stremio add-ons (such as Torrentio or AIOStreams) in Settings.",
                        actionText = "Manage Add-ons",
                        actionIcon = Icons.Default.Settings,
                        focusRequester = initialFocusRequester,
                        onActionClick = onNavigateToAddons
                    )
                }

                // ERROR OR ALL PROVIDERS FAILED
                uiState.errorMessage != null && !uiState.hasStreams -> {
                    StreamInfoCard(
                        title = "Unable to Load Streams",
                        description = uiState.errorMessage ?: "Failed to connect to stream providers.",
                        actionText = "Retry",
                        actionIcon = Icons.Default.Refresh,
                        focusRequester = initialFocusRequester,
                        onActionClick = { viewModel.retry(mediaId, mediaType, season, episode) }
                    )
                }

                // NO STREAMS FOUND
                !uiState.hasStreams -> {
                    StreamInfoCard(
                        title = "No Streams Found",
                        description = "Checked all enabled add-ons, but no streaming sources were returned for this title.",
                        actionText = "Retry Lookup",
                        actionIcon = Icons.Default.Refresh,
                        focusRequester = initialFocusRequester,
                        onActionClick = { viewModel.retry(mediaId, mediaType, season, episode) }
                    )
                }

                // SUCCESSFUL STREAM RESULTS LIST
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (uiState.selectedStream != null) {
                            SelectedStreamBanner(
                                selectedStream = uiState.selectedStream!!,
                                onClear = { viewModel.clearSelection() }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            uiState.providerResults.forEachIndexed { providerIndex, providerResult ->
                                if (providerResult.streams.isNotEmpty()) {
                                    item {
                                        ProviderHeader(providerName = providerResult.addonName)
                                    }

                                    items(
                                        items = providerResult.streams,
                                        key = { stream -> "${providerResult.addonId}_${providerResult.streams.indexOf(stream)}_${stream.url ?: ""}_${stream.name ?: ""}_${stream.title ?: ""}" }
                                    ) { stream ->
                                        val isFirst = providerIndex == 0 && providerResult.streams.first() == stream
                                        StreamItemCard(
                                            stream = stream,
                                            providerName = providerResult.addonName,
                                            isSelected = uiState.selectedStream == stream,
                                            focusRequester = if (isFirst) initialFocusRequester else remember { FocusRequester() },
                                            onClick = {
                                                viewModel.selectStream(stream)
                                                onStreamSelected(stream)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderHeader(providerName: String) {
    Text(
        text = providerName.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.6f),
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamItemCard(
    stream: AddonStream,
    providerName: String,
    isSelected: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    val displayTitle = stream.name?.takeIf { it.isNotBlank() }
        ?: stream.title?.takeIf { it.isNotBlank() }
        ?: providerName

    val displayDesc = stream.title?.takeIf { it.isNotBlank() && it != displayTitle }
        ?: stream.description?.takeIf { it.isNotBlank() }
        ?: stream.url ?: "Direct Media Stream"

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        colors = ButtonDefaults.colors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color.White.copy(alpha = 0.08f),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.02f),
        shape = ButtonDefaults.shape(RoundedCornerShape(10.dp)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2563EB), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = providerName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = displayTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = displayDesc,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF22C55E), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Selected",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedStreamBanner(selectedStream: AddonStream, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(8.dp))
            .background(Color(0xFF065F46).copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Stream Selected (Phase 4C UI Mode)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22C55E)
                    )
                    Text(
                        text = selectedStream.name ?: selectedStream.title ?: "Ready for Phase 4D Playback",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamInfoCard(
    title: String,
    description: String,
    actionText: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    focusRequester: FocusRequester,
    onActionClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.75f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onActionClick,
                modifier = Modifier.focusRequester(focusRequester),
                colors = ButtonDefaults.colors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Color.Black
                ),
                scale = ButtonDefaults.scale(focusedScale = 1.05f),
                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actionText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
