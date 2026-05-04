package com.example.peak.ui.components.sidebar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SidebarItem(
    itemType: SidebarItemType,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (label, icon) = when (itemType) {
        SidebarItemType.HOME -> "Home" to Icons.Default.Home
        SidebarItemType.MOVIES -> "Movies" to Icons.Default.PlayArrow
        SidebarItemType.TV -> "TV Shows" to Icons.Default.List
        SidebarItemType.SETTINGS -> "Settings" to Icons.Default.Settings
    }

    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.Gray,
            focusedContentColor = Color.White
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = label, 
                modifier = Modifier.size(28.dp)
            )
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
