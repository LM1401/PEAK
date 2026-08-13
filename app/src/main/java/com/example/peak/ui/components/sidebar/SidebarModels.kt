package com.example.peak.ui.components.sidebar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class SidebarItemType(
    val label: String,
    val icon: ImageVector
) {
    PROFILE("Profile", Icons.Default.Person),
    HOME("Home", Icons.Default.Home),
    SEARCH("Search", Icons.Default.Search),
    MOVIES("Movies", Icons.Default.PlayArrow),
    TV("TV Shows", Icons.AutoMirrored.Filled.List),
    MY_LIST("My List", Icons.Default.Add),
    SETTINGS("Settings", Icons.Default.Settings)
}
