package com.example.peak.ui.components.sidebar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Sidebar(
    selectedItem: SidebarItemType,
    onItemSelected: (SidebarItemType) -> Unit,
    onMoveRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 280.dp else 80.dp,
        animationSpec = tween(durationMillis = 250),
        label = "railWidth"
    )

    // Cinematic Navy/Blue-Black Gradient for Sidebar
    val sidebarBackground = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF040B16).copy(alpha = 0.95f),
            Color(0xFF040B16).copy(alpha = 0.85f),
            Color.Transparent
        )
    )

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
            .onFocusChanged {
                isExpanded = it.hasFocus
            }
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionRight) {
                    onMoveRight()
                    true
                } else {
                    false
                }
            },
        colors = SurfaceDefaults.colors(
            containerColor = Color.Transparent // Background handled by Box for gradient
        ),
        shape = RectangleShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(sidebarBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // PEAK LOGO
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .height(40.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "PEAK",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // SIDEBAR ITEMS
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SidebarItemType.entries.forEach { itemType ->
                        if (itemType != SidebarItemType.PROFILE && itemType != SidebarItemType.SETTINGS) {
                            SidebarItem(
                                itemType = itemType,
                                isSelected = selectedItem == itemType,
                                isExpanded = isExpanded,
                                onClick = { onItemSelected(itemType) }
                            )
                        } else if (itemType == SidebarItemType.PROFILE) {
                            // Specialized Profile Item at top
                            SidebarProfileItem(
                                isSelected = selectedItem == itemType,
                                isExpanded = isExpanded,
                                onClick = { onItemSelected(itemType) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // SETTINGS at bottom
                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                    SidebarItem(
                        itemType = SidebarItemType.SETTINGS,
                        isSelected = selectedItem == SidebarItemType.SETTINGS,
                        isExpanded = isExpanded,
                        onClick = { onItemSelected(SidebarItemType.SETTINGS) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SidebarProfileItem(
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RectangleShape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SidebarItemType.PROFILE.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Guest",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}
