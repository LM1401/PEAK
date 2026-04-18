package com.example.peak.ui.screens.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.rememberAsyncImagePainter
import com.example.peak.data.remote.dto.toMovie
import com.example.peak.data.remote.retrofit.RetrofitInstance
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.sampleRows
import com.example.peak.ui.components.MovieCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A Netflix-style Movie Detail Screen for Android TV.
 * Matches the requested layout: Title, Metadata, Description, Cast/Director/Genres, and Vertical Actions.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflixDetailScreen(
    movieId: String,
    onPlayClick: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    var movie by remember { mutableStateOf<Movie?>(null) }
    var similarMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(movieId) {
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitInstance.api.getTrending()
            }
            val foundMovie = response.results.find { it.id.toString() == movieId }?.toMovie()
            
            movie = foundMovie
            similarMovies = response.results.take(10).map { it.toMovie() }
            isLoading = false
        } catch (e: Exception) {
            movie = sampleRows().firstOrNull()?.movies?.firstOrNull()
            similarMovies = sampleRows().flatMap { it.movies }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Loading...", color = Color.White)
        }
    } else {
        movie?.let { currentMovie ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // 1. Cinematic Backdrop
                Image(
                    painter = rememberAsyncImagePainter(currentMovie.backdropUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .align(Alignment.TopCenter),
                    contentScale = ContentScale.Crop
                )

                // 2. Gradients for cinematic feel and text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.95f),
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent
                                ),
                                endX = 1400f
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Black
                                ),
                                startY = 300f
                            )
                        )
                )

                // 3. Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 58.dp)
                ) {
                    Spacer(modifier = Modifier.height(100.dp))

                    // Header Label
                    Text(
                        text = "A PEAK ORIGINAL",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    // Title
                    Text(
                        text = currentMovie.name.uppercase(),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Metadata Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "${(currentMovie.rating.toDoubleOrNull()?.times(10))?.toInt() ?: 92}% Match",
                            color = Color(0xFF46D369),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(text = currentMovie.year, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(text = currentMovie.ageRating, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Text(text = currentMovie.duration, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(text = "HD", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(text = "5.1", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Description
                    Text(
                        text = currentMovie.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 700.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cast, Director, Genres
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        DetailInfoText(label = "Cast", value = currentMovie.cast)
                        DetailInfoText(label = "Director", value = currentMovie.director)
                        DetailInfoText(label = "Genres", value = currentMovie.genres)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Action Buttons (Vertical)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.width(320.dp)
                    ) {
                        ActionButton(
                            icon = Icons.Default.PlayArrow,
                            text = "Play",
                            onClick = { onPlayClick(currentMovie) }
                        )
                        ActionButton(
                            icon = Icons.Default.PlayArrow,
                            text = "Play Trailer",
                            onClick = { /* Play Trailer logic */ }
                        )
                        ActionButton(
                            icon = Icons.AutoMirrored.Filled.List,
                            text = "Audio and Subtitles",
                            onClick = { /* Audio settings */ }
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.2f)))
                        Spacer(modifier = Modifier.height(4.dp))

                        ActionButton(
                            icon = Icons.Default.Add,
                            text = "Add to My List",
                            onClick = { /* Add to list logic */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(60.dp))

                    // "More Like This" Section
                    Text(
                        text = "More Like This",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(similarMovies) { simMovie ->
                            MovieCard(
                                movie = simMovie,
                                onMovieFocused = {},
                                onMovieClick = { onMovieClick(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailInfoText(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        shape = ButtonDefaults.shape(RoundedCornerShape(4.dp)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
