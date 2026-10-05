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
import com.example.peak.domain.model.MediaType
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.ui.screens.detail.DetailScreen
import com.example.peak.ui.screens.home.HomeScreen
import com.example.peak.ui.screens.player.PlayerScreen
import com.example.peak.ui.theme.PEAKTheme

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
    val context = androidx.compose.ui.platform.LocalContext.current

    val movieRepository = remember { PeakDependencyProvider.getMovieRepository() }
    val continueWatchingRepository = remember { PeakDependencyProvider.getContinueWatchingRepository(context) }

    val onSidebarItemSelected: (SidebarItemType) -> Unit = { item ->
        when (item) {
            SidebarItemType.HOME -> navController.navigate("home") {
                popUpTo("home") { inclusive = true }
                launchSingleTop = true
            }
            SidebarItemType.MOVIES -> navController.navigate("movies") {
                launchSingleTop = true
            }
            SidebarItemType.TV -> navController.navigate("series") {
                launchSingleTop = true
            }
            SidebarItemType.SEARCH -> navController.navigate("search") {
                launchSingleTop = true
            }
            SidebarItemType.SETTINGS -> navController.navigate("settings") {
                launchSingleTop = true
            }
            SidebarItemType.MY_LIST -> {
                // navController.navigate("mylist")
            }
            SidebarItemType.PROFILE -> {
                // navController.navigate("profile")
            }
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
                onSidebarItemSelected = onSidebarItemSelected,
                onMovieClick = { movie ->
                    navController.navigate("detail/${movie.movieId}/${movie.mediaType.name}")
                }
            )
        }

        composable("movies") {
            val moviesViewModel: com.example.peak.ui.screens.movies.MoviesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.screens.movies.MoviesViewModelFactory(movieRepository)
            )
            com.example.peak.ui.screens.movies.MoviesScreen(
                viewModel = moviesViewModel,
                onSidebarItemSelected = onSidebarItemSelected,
                onMovieClick = { movie ->
                    navController.navigate("detail/${movie.movieId}/${movie.mediaType.name}")
                }
            )
        }

        composable("series") {
            val seriesViewModel: com.example.peak.ui.screens.series.SeriesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.screens.series.SeriesViewModelFactory(movieRepository)
            )
            com.example.peak.ui.screens.series.SeriesScreen(
                viewModel = seriesViewModel,
                onSidebarItemSelected = onSidebarItemSelected,
                onMovieClick = { movie ->
                    navController.navigate("detail/${movie.movieId}/${movie.mediaType.name}")
                }
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
            route = "detail/{mediaId}/{mediaType}",
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("mediaType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
            val mediaTypeStr = backStackEntry.arguments?.getString("mediaType") ?: "MOVIE"
            val mediaType = try { MediaType.valueOf(mediaTypeStr) } catch (e: Exception) { MediaType.MOVIE }
            
            DetailScreen(
                mediaId = mediaId,
                mediaType = mediaType,
                movieRepository = movieRepository,
                continueWatchingRepository = continueWatchingRepository,
                onPlayClick = { movie ->
                    navController.navigate("player/${movie.movieId}/${movie.mediaType.name}")
                },
                onMovieClick = { movie ->
                    // Replace current detail instead of stacking
                    navController.navigate("detail/${movie.movieId}/${movie.mediaType.name}") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onSidebarItemSelected = onSidebarItemSelected
            )
        }

        composable(
            route = "player/{mediaId}/{mediaType}",
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("mediaType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
            val mediaTypeStr = backStackEntry.arguments?.getString("mediaType") ?: "MOVIE"
            val mediaType = try { MediaType.valueOf(mediaTypeStr) } catch (e: Exception) { MediaType.MOVIE }
            
            PlayerScreen(
                mediaId = mediaId,
                mediaType = mediaType,
                onPlaybackFinished = { navController.popBackStack() },
                continueWatchingRepository = continueWatchingRepository,
                movieRepository = movieRepository
            )
        }

        composable("search") {
            val searchViewModel: com.example.peak.ui.search.SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.search.SearchViewModelFactory(movieRepository)
            )
            com.example.peak.ui.search.SearchScreen(
                viewModel = searchViewModel,
                onSidebarItemSelected = onSidebarItemSelected,
                onItemClick = { item ->
                    navController.navigate("detail/${item.movieId}/${item.mediaType.name}")
                },
                onDiscoveryClick = { title, genreIds ->
                    navController.navigate("discovery_results/$title/$genreIds")
                },
                onLearnMoreClick = {
                    navController.navigate("all_genres")
                }
            )
        }

        composable(
            route = "discovery_results/{title}/{genreIds}",
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("genreIds") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: "Results"
            val genreIds = backStackEntry.arguments?.getString("genreIds") ?: ""
            
            val viewModel: com.example.peak.ui.search.DiscoveryResultsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.search.DiscoveryResultsViewModelFactory(movieRepository)
            )
            
            // Load the results once when navigating
            LaunchedEffect(title, genreIds) {
                viewModel.loadDiscoveryResults(title, genreIds)
            }
            
            com.example.peak.ui.search.DiscoveryResultsScreen(
                viewModel = viewModel,
                onSidebarItemSelected = onSidebarItemSelected,
                onItemClick = { item ->
                    navController.navigate("detail/${item.movieId}/${item.mediaType.name}")
                }
            )
        }

        composable("all_genres") {
            com.example.peak.ui.search.AllGenresScreen(
                onSidebarItemSelected = onSidebarItemSelected,
                onGenreClick = { title, genreIds ->
                    navController.navigate("discovery_results/$title/$genreIds")
                }
            )
        }
    }
}
