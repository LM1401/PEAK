package com.example.peak

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.*
import com.example.peak.ui.screens.detail.NetflixDetailScreen
import com.example.peak.ui.screens.home.HomeScreen
import com.example.peak.ui.screens.player.PlayerScreen
import com.example.peak.ui.theme.PEAKTheme
import com.example.peak.ui.search.toMovie

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

    val movieRepository = com.example.peak.data.repository.MovieRepositoryImpl(
        com.example.peak.data.remote.retrofit.RetrofitInstance.api
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    val continueWatchingStorage = remember {
        com.example.peak.data.continuewatching.ContinueWatchingStorage(context)
    }
    val continueWatchingRepository = remember {
        com.example.peak.data.repository.ContinueWatchingRepository(continueWatchingStorage)
    }

    val onTabSelected: (String) -> Unit = { tab ->
        when (tab) {
            "Home" -> navController.navigate("home") {
                popUpTo("home") { inclusive = true }
                launchSingleTop = true
            }
            "Series" -> navController.navigate("series") {
                launchSingleTop = true
            }
            "My Watchlist" -> {} // Handle later
            "Films" -> navController.navigate("movies") {
                launchSingleTop = true
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController, 
        startDestination = "home"
    ) {
        composable("home") {
            val homeViewModel: com.example.peak.ui.screens.home.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.screens.home.HomeViewModelFactory(movieRepository, continueWatchingRepository)
            )
            HomeScreen(
                viewModel = homeViewModel,
                onTabSelected = onTabSelected,
                onMovieClick = { movie ->
                    navController.navigate("movie_detail/${movie.movieId}")
                },
                onSettingsClick = { navController.navigate("settings") },
                onSearchClick = { navController.navigate("search") }
            )
        }

        composable("movies") {
            val moviesViewModel: com.example.peak.ui.screens.movies.MoviesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.screens.movies.MoviesViewModelFactory(movieRepository)
            )
            com.example.peak.ui.screens.movies.MoviesScreen(
                viewModel = moviesViewModel,
                onTabSelected = onTabSelected,
                onMovieClick = { movie ->
                    navController.navigate("movie_detail/${movie.movieId}")
                },
                onSettingsClick = { navController.navigate("settings") },
                onSearchClick = { navController.navigate("search") }
            )
        }

        composable("series") {
            val seriesViewModel: com.example.peak.ui.screens.series.SeriesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.screens.series.SeriesViewModelFactory(movieRepository)
            )
            com.example.peak.ui.screens.series.SeriesScreen(
                viewModel = seriesViewModel,
                onTabSelected = onTabSelected,
                onMovieClick = { movie ->
                    navController.navigate("movie_detail/${movie.movieId}")
                },
                onSettingsClick = { navController.navigate("settings") },
                onSearchClick = { navController.navigate("search") }
            )
        }

        composable("settings") {
            val settingsViewModel: com.example.peak.ui.settings.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            com.example.peak.ui.settings.SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "movie_detail/{movieId}",
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            NetflixDetailScreen(
                movieId = movieId,
                movieRepository = movieRepository,
                continueWatchingRepository = continueWatchingRepository,
                onPlayClick = { movie ->
                    navController.navigate("player/${movie.movieId}")
                },
                onMovieClick = { movie ->
                    // Replace current detail instead of stacking
                    navController.navigate("movie_detail/${movie.movieId}") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }

        // 5. Proper Player Route (Refactored to Option A)
        composable(
            route = "player/{movieId}",
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            
            PlayerScreen(
                movieId = movieId,
                onPlaybackFinished = { navController.popBackStack() },
                continueWatchingRepository = continueWatchingRepository,
                movieRepository = movieRepository
            )
        }

        composable("search") {
            val okHttpClient = okhttp3.OkHttpClient()
            val searchRepository = com.example.peak.data.search.SearchRepository(okHttpClient)
            val searchViewModel: com.example.peak.ui.search.SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.search.SearchViewModelFactory(searchRepository, movieRepository)
            )
            com.example.peak.ui.search.SearchScreen(
                viewModel = searchViewModel,
                onItemClick = { item ->
                    navController.navigate("movie_detail/${item.id}")
                }
            )
        }
    }
}
