package com.example.peak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopNavigationBar(
    modifier: Modifier = Modifier,
    selectedTab: String = "Home",
    onTabSelected: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val tabs = listOf("Home", "Series", "Films", "My Watchlist")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Avatar (Left)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE50914)),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.8f)))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation Tabs Grouped in Pill (Center)
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)) // Glass Effect
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                onClick = onSearchClick,
                modifier = Modifier.size(32.dp),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White,
                    focusedContentColor = Color.White
                ),
                shape = ClickableSurfaceDefaults.shape(CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                }
            }

            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                Surface(
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.height(32.dp),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                        focusedContainerColor = Color.White,
                        contentColor = Color.White,
                        focusedContentColor = Color.Black
                    ),
                    shape = ClickableSurfaceDefaults.shape(CircleShape)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Text(tab, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Settings Button (Right side)
        Surface(
            onClick = onSettingsClick,
            modifier = Modifier.size(36.dp),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.White.copy(alpha = 0.2f),
                contentColor = Color.White,
                focusedContentColor = Color.White
            ),
            shape = ClickableSurfaceDefaults.shape(CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
