package com.example.peak.ui.search

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.ui.screens.home.HomeConstants

@Composable
fun AllGenresScreen(
    onSidebarItemSelected: (SidebarItemType) -> Unit,
    onGenreClick: (String, String) -> Unit
) {
    val navFocusRequester = remember { FocusRequester() }
    val categories = remember { SearchDiscoveryConfig.allCategories }
    val cardFocusRequesters = remember(categories.size) { List(categories.size) { FocusRequester() } }

    var focusedIndex by remember { mutableIntStateOf(0) }
    var isInitialFocusRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isInitialFocusRequested && cardFocusRequesters.isNotEmpty()) {
            try {
                cardFocusRequesters[0].requestFocus()
                isInitialFocusRequested = true
            } catch (e: Exception) {}
        }
    }

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.SEARCH,
        onSidebarItemSelected = onSidebarItemSelected,
        focusedMovieProvider = { null },
        navFocusRequester = navFocusRequester,
        contentFocusRequester = if (cardFocusRequesters.isNotEmpty()) cardFocusRequesters[0] else navFocusRequester,
        heroHud = null
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = HomeConstants.HOME_CONTENT_START_PADDING - 14.dp, // 86.dp start to accommodate 14.dp focus safety inset
                    top = 32.dp,
                    end = 48.dp - 14.dp // 34.dp end to accommodate 14.dp focus safety inset
                )
        ) {
            // FIXED HEADER
            Text(
                text = "I'M FEELING LIKE A",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 6.sp,
                modifier = Modifier.padding(
                    start = 14.dp, // Aligns header with normal card content start at 100.dp
                    bottom = 16.dp
                )
            )

            // SLOT-ALIGNED CAMERA VIEWPORT WITH FOCUS SAFETY ENVELOPE
            // 3 rows * 128dp + 2 gaps * 20dp = 424dp category height.
            // Viewport height = 444dp (424dp + 10dp top safety + 10dp bottom safety).
            // Clipped bounds = 1160dp width x 444dp height.
            // Inner content padding (horizontal = 14.dp, vertical = 10.dp) keeps unfocused cards at exact 1132dp x 424dp geometry.
            val density = LocalDensity.current

            val rowIndex = focusedIndex / 3
            val visibleRows = 3
            val totalRows = (categories.size + 2) / 3 // 7 rows for 20 items
            val topRow = (rowIndex - 2).coerceIn(0, (totalRows - visibleRows).coerceAtLeast(0))
            val targetCameraOffsetY = -(topRow * 148).dp

            val cameraOffsetY by animateDpAsState(
                targetValue = targetCameraOffsetY,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "all_genres_camera"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(444.dp)
                    .clipToBounds()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(
                            unbounded = true,
                            align = Alignment.Top
                        )
                        .graphicsLayer {
                            translationY = with(density) { cameraOffsetY.toPx() }
                        },
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val chunkedCategories = remember(categories) { categories.chunked(3) }

                    chunkedCategories.forEachIndexed { rIndex, rowCategories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            rowCategories.forEachIndexed { cIndex, category ->
                                val index = rIndex * 3 + cIndex
                                val focusRequester = cardFocusRequesters[index]

                                DiscoveryButton(
                                    category = category,
                                    onClick = { onGenreClick(category.label, category.tmdbGenreIds) },
                                    iconSize = 96.dp,
                                    horizontalPadding = 20.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(128.dp)
                                        .focusRequester(focusRequester)
                                        .onFocusChanged {
                                            if (it.hasFocus) {
                                                focusedIndex = index
                                            }
                                        }
                                        .focusProperties {
                                            // LEFT BOUNDARY
                                            left = if (index % 3 == 0) {
                                                navFocusRequester
                                            } else {
                                                cardFocusRequesters[index - 1]
                                            }

                                            // RIGHT BOUNDARY
                                            right = if (index % 3 == 2 || index == categories.lastIndex) {
                                                FocusRequester.Cancel
                                            } else if (index + 1 < categories.size) {
                                                cardFocusRequesters[index + 1]
                                            } else {
                                                FocusRequester.Default
                                            }

                                            // UP BOUNDARY
                                            up = if (index >= 3) {
                                                cardFocusRequesters[index - 3]
                                            } else {
                                                FocusRequester.Cancel
                                            }

                                            // DOWN BOUNDARY
                                            down = if (index + 3 < categories.size) {
                                                cardFocusRequesters[index + 3]
                                            } else if (index in 15..17) {
                                                if (index == 15) cardFocusRequesters[18]
                                                else cardFocusRequesters[19]
                                            } else {
                                                FocusRequester.Cancel
                                            }
                                        }
                                )
                            }

                            // Fill remaining empty columns in the last row if row has fewer than 3 items
                            if (rowCategories.size < 3) {
                                repeat(3 - rowCategories.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
