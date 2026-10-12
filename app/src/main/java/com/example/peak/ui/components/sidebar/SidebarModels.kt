package com.example.peak.ui.components.sidebar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val CasinoDiceIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Casino",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19.0f, 3.0f)
            lineTo(5.0f, 3.0f)
            curveTo(3.9f, 3.0f, 3.0f, 3.9f, 3.0f, 5.0f)
            lineTo(3.0f, 19.0f)
            curveTo(3.0f, 20.1f, 3.9f, 21.0f, 5.0f, 21.0f)
            lineTo(19.0f, 21.0f)
            curveTo(20.1f, 21.0f, 21.0f, 20.1f, 21.0f, 19.0f)
            lineTo(21.0f, 5.0f)
            curveTo(21.0f, 3.9f, 20.1f, 3.0f, 19.0f, 3.0f)
            close()
            moveTo(7.5f, 18.0f)
            curveTo(6.67f, 18.0f, 6.0f, 17.33f, 6.0f, 16.5f)
            reflectiveCurveTo(6.67f, 15.0f, 7.5f, 15.0f)
            reflectiveCurveTo(9.0f, 15.67f, 9.0f, 16.5f)
            reflectiveCurveTo(8.33f, 18.0f, 7.5f, 18.0f)
            close()
            moveTo(7.5f, 9.0f)
            curveTo(6.67f, 9.0f, 6.0f, 8.33f, 6.0f, 7.5f)
            reflectiveCurveTo(6.67f, 6.0f, 7.5f, 6.0f)
            reflectiveCurveTo(9.0f, 6.67f, 9.0f, 7.5f)
            reflectiveCurveTo(8.33f, 9.0f, 7.5f, 9.0f)
            close()
            moveTo(12.0f, 13.5f)
            curveTo(11.17f, 13.5f, 10.5f, 12.83f, 10.5f, 12.0f)
            reflectiveCurveTo(11.17f, 10.5f, 12.0f, 10.5f)
            reflectiveCurveTo(13.5f, 11.17f, 13.5f, 12.0f)
            reflectiveCurveTo(12.83f, 13.5f, 12.0f, 13.5f)
            close()
            moveTo(16.5f, 18.0f)
            curveTo(15.67f, 18.0f, 15.0f, 17.33f, 15.0f, 16.5f)
            reflectiveCurveTo(15.67f, 15.0f, 16.5f, 15.0f)
            reflectiveCurveTo(18.0f, 15.67f, 18.0f, 16.5f)
            reflectiveCurveTo(17.33f, 18.0f, 16.5f, 18.0f)
            close()
            moveTo(16.5f, 9.0f)
            curveTo(15.67f, 9.0f, 15.0f, 8.33f, 15.0f, 7.5f)
            reflectiveCurveTo(15.67f, 6.0f, 16.5f, 6.0f)
            reflectiveCurveTo(18.0f, 6.67f, 18.0f, 7.5f)
            reflectiveCurveTo(17.33f, 9.0f, 16.5f, 9.0f)
            close()
        }
    }.build()

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
    RANDOM("Random", CasinoDiceIcon),
    SETTINGS("Settings", Icons.Default.Settings)
}
