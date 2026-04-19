package com.example.peak

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.rememberAsyncImagePainter
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.peak.data.remote.dto.toMovie
import com.example.peak.data.remote.retrofit.RetrofitInstance
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.model.sampleRows
import com.example.peak.ui.components.HomeMovieCard
import com.example.peak.ui.screens.detail.NetflixDetailScreen
import com.example.peak.ui.theme.PEAKTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PEAK", "MainActivity onCreate")

        setContent {
            PEAKTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    PEAKApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PEAKApp() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onMovieClick = { movie ->
                    Log.d("MOVIE_CLICK", "Clicked: ${movie.movieId}")
                    navController.navigate("movie_detail/${movie.movieId}")
                }
            )
        }
        composable(
            route = "movie_detail/{movieId}",
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            NetflixDetailScreen(
                movieId = movieId,
                onPlayClick = { /* Navigate to player */ },
                onMovieClick = { movie ->
                    navController.navigate("movie_detail/${movie.movieId}") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(onMovieClick: (Movie) -> Unit) {
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var focusedMovie by remember { mutableStateOf<Movie?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitInstance.api.getTrending()
            }
            val fetchedMovies = response.results.map { it.toMovie() }
            movies = fetchedMovies
            if (fetchedMovies.isNotEmpty()) {
                focusedMovie = fetchedMovies.first()
            }
            isLoading = false
        } catch (e: Exception) {
            Log.e("PEAK", "Error fetching movies", e)
            movies = sampleRows().flatMap { it.movies }
            if (movies.isNotEmpty()) focusedMovie = movies.first()
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Loading PEAK...", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }
    } else {
        val movieRows = remember(movies) {
            if (movies.size >= 10) {
                listOf(
                    Row("Trending This Week", movies.subList(0, 10)),
                    Row("Recommended for You", movies.subList(10, movies.size.coerceAtMost(20)))
                )
            } else {
                listOf(Row("Trending", movies))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            BoxWithConstraints {
                val heroHeight = maxHeight * 0.35f
                HeroSection(
                    movie = focusedMovie,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                itemsIndexed(movieRows) { _, row ->
                    MovieRow(
                        row = row,
                        onMovieFocused = { movie -> focusedMovie = movie },
                        onMovieClick = onMovieClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroSection(
    movie: Movie?,
    modifier: Modifier = Modifier
) {
    movie?.let { currentMovie ->
        Box(
            modifier = modifier
                .focusable(false)
        ) {
            Crossfade(targetState = currentMovie, animationSpec = tween(800)) { target ->
                Image(
                    painter = rememberAsyncImagePainter(target.backdropUrl),
                    contentDescription = target.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent
                            )
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
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 24.dp)
                    .fillMaxWidth(0.7f)
            ) {
                Text(
                    text = currentMovie.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentMovie.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = currentMovie.ageRating,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieRow(
    row: Row, 
    onMovieFocused: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = row.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(start = 58.dp, bottom = 8.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 58.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(row.movies) { _, movie ->
                HomeMovieCard(
                    movie = movie,
                    onMovieFocused = onMovieFocused,
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}
