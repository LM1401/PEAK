package com.example.peak.ui.components.sidebar

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SidebarItem(
    itemType: SidebarItemType,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.6f),
            focusedContainerColor = Color.White.copy(alpha = 0.2f),
            focusedContentColor = Color.White
        ),
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RectangleShape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = itemType.icon,
                contentDescription = itemType.label,
                modifier = Modifier.size(24.dp)
            )
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(200, delayMillis = 100)) + 
                        expandHorizontally(animationSpec = tween(200, delayMillis = 100)),
                exit = fadeOut(animationSpec = tween(100))
            ) {
                Row {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = itemType.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
