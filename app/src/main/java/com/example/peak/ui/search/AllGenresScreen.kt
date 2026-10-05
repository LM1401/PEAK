package com.example.peak.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
    val gridFocusRequester = remember { FocusRequester() }

    var isInitialFocusRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isInitialFocusRequested) {
            try {
                gridFocusRequester.requestFocus()
                isInitialFocusRequested = true
            } catch (e: Exception) {}
        }
    }

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.SEARCH,
        onSidebarItemSelected = onSidebarItemSelected,
        focusedMovieProvider = { null },
        navFocusRequester = navFocusRequester,
        contentFocusRequester = gridFocusRequester,
        heroHud = null
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = HomeConstants.HOME_CONTENT_START_PADDING,
                    top = 64.dp,
                    end = 48.dp
                )
        ) {
            Text(
                text = "ALL CATEGORIES",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Light,
                letterSpacing = 8.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(SearchDiscoveryConfig.allCategories) { index, category ->
                    DiscoveryButton(
                        category = category,
                        onClick = { onGenreClick(category.label, category.tmdbGenreIds) },
                        modifier = Modifier
                            .height(64.dp)
                            .focusProperties {
                                if (index % 4 == 0) left = navFocusRequester
                            }
                            .let { if (index == 0) it.focusRequester(gridFocusRequester) else it }
                    )
                }
            }
        }
    }
}