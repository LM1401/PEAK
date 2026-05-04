package com.example.peak.ui.components.sidebar

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
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
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
            focusedContentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.15f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = itemType.icon,
                contentDescription = itemType.label,
                modifier = Modifier.size(26.dp)
            )
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(150, delayMillis = 100)) + 
                        slideInHorizontally(animationSpec = tween(150, delayMillis = 100)) { -10 },
                exit = fadeOut(animationSpec = tween(100))
            ) {
                Row {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = itemType.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
