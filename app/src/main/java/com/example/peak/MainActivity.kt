package com.example.peak

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.*
import com.example.peak.ui.screens.detail.NetflixDetailScreen
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    
    val currentRoute = navBackStackEntry?.destination?.route

    Row(modifier = Modifier.fillMaxSize()) {
        SideNavigationRail(navController = navController, currentRoute = currentRoute)

        // 4. NavHost in a stable container to prevent recreation
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController, 
                startDestination = "home"
            ) {
                composable("home") {
                    val movieRepository = com.example.peak.data.repository.MovieRepositoryImpl(
                        com.example.peak.data.remote.retrofit.RetrofitInstance.api
                    )
                    val homeViewModel: com.example.peak.ui.screens.home.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = com.example.peak.ui.screens.home.HomeViewModelFactory(movieRepository)
                    )
                    HomeScreen(
                        viewModel = homeViewModel,
                        onMovieClick = { movie ->
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

                // 5. Proper Player Route
                composable(
                    route = "player/{movieId}",
                    arguments = listOf(navArgument("movieId") { type = NavType.StringType })
                ) { _ ->
                    // Using a placeholder URL as per existing PlayerScreen requirement
                    PlayerScreen(
                        videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        onPlaybackFinished = { navController.popBackStack() }
                    )
                }

                composable("search") {
                    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        Text("Search Screen Placeholder", color = Color.White)
                    }
                }
            }
        }
    }
}

// 1. & 6. Refined SideNavigationRail for TV focus stability
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SideNavigationRail(
    navController: NavController,
    currentRoute: String?
) {
    if (currentRoute?.startsWith("player") == true) return

    var isExpanded by remember { mutableStateOf(false) }

    // FIX 3 — ENSURE PROPER RAIL STATE LOGIC
    val isPlayer = currentRoute?.startsWith("player") == true
    val isDetail = currentRoute?.startsWith("movie_detail") == true

    // FIX 5 — FIX ALPHA BEHAVIOR (SEPARATE FROM COLOR)
    val railAlpha by animateFloatAsState(
        targetValue = when {
            isPlayer -> 0f
            isDetail -> 0.25f
            else -> 1f
        },
        label = "railAlpha"
    )

    // FIX 4 — FIX WIDTH BEHAVIOR (SINGLE SOURCE OF TRUTH)
    val railWidth by animateDpAsState(
        targetValue = when {
            isPlayer -> 0.dp
            isDetail -> 72.dp
            else -> if (isExpanded) 200.dp else 72.dp
        },
        label = "railWidth"
    )

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(railWidth)
            .alpha(railAlpha)
            .onFocusChanged {
                isExpanded = it.hasFocus
            },
        // FIX 1 — REMOVE ALPHA INSIDE SURFACE COLOR
        colors = SurfaceDefaults.colors(
            containerColor = Color(0xFF121212)
        ),
        shape = RectangleShape
    ) {
        val canExpand = !isPlayer && !isDetail
        Column(
            modifier = Modifier.padding(vertical = 48.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            NavigationRailItem(
                label = "Home",
                icon = Icons.Default.Home,
                isSelected = currentRoute == "home",
                isExpanded = isExpanded && canExpand,
                onClick = { 
                    navController.safeNavigate("home")
                }
            )
            NavigationRailItem(
                label = "Search",
                icon = Icons.Default.Search,
                isSelected = currentRoute == "search",
                isExpanded = isExpanded && canExpand,
                onClick = { 
                    navController.safeNavigate("search")
                }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NavigationRailItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.Gray,
            focusedContentColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
            if (isExpanded) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label, 
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}

// 3. Safe Navigation Helper
fun NavController.safeNavigate(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
