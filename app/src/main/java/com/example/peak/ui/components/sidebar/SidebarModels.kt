package com.example.peak.ui.components.sidebar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class SidebarItemType(
    val label: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Default.Home),
    MOVIES("Movies", Icons.Default.PlayArrow),
    TV("TV Shows", Icons.AutoMirrored.Filled.List),
    SETTINGS("Settings", Icons.Default.Settings)
}
