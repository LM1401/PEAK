package com.example.peak.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopNavigationBar(
    modifier: Modifier = Modifier,
    selectedTab: String = "Home",
    onTabSelected: (String) -> Unit = {}
) {
    val tabs = listOf("Home", "Series", "Films", "Games", "My Netflix")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF2E5BFF)), // Blue profile color
            contentAlignment = Alignment.Center
        ) {
            // Placeholder for profile icon
            Text("L", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation Tabs
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Icon
            IconButton(onClick = { /* Search */ }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }

            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                Surface(
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.height(36.dp),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                        focusedContainerColor = Color.White,
                        contentColor = if (isSelected) Color.White else Color.LightGray,
                        focusedContentColor = Color.Black
                    ),
                    shape = ClickableSurfaceDefaults.shape(CircleShape)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Netflix Logo (Right side)
        Text(
            text = "N",
            color = Color.Red,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )
    }
}
