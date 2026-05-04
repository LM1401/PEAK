package com.example.peak.ui.components.sidebar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
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
        targetValue = if (isExpanded) 200.dp else 72.dp,
        animationSpec = tween(durationMillis = 200),
        label = "railWidth"
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
            containerColor = Color(0xFF121212)
        ),
        shape = RectangleShape
    ) {
        Column(
            modifier = Modifier.padding(vertical = 48.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            SidebarItemType.entries.forEach { itemType ->
                SidebarItem(
                    itemType = itemType,
                    isSelected = selectedItem == itemType,
                    isExpanded = isExpanded,
                    onClick = { onItemSelected(itemType) }
                )
            }
        }
    }
}
