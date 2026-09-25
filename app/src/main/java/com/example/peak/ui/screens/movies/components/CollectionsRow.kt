package com.example.peak.ui.screens.movies.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.peak.ui.focus.FocusMemoryManager
import com.example.peak.ui.screens.home.HomeConstants
import com.example.peak.ui.screens.movies.CollectionItem
import com.example.peak.ui.screens.movies.CollectionsSectionState
import kotlinx.coroutines.launch

/**
 * Collections Row Carousel (Phase 3 Target Design).
 * Renders horizontal "Collections" carousel with deterministic FocusRequester navigation and focus memory.
 */
@Composable
fun CollectionsRow(
    collectionsState: CollectionsSectionState,
    onCollectionClick: (CollectionItem) -> Unit,
    modifier: Modifier = Modifier,
    onCollectionFocused: ((String, CollectionItem) -> Unit)? = null,
    rowId: String = "movies_collections",
    navFocusRequester: FocusRequester? = null,
    getRequester: ((String, Int) -> FocusRequester)? = null,
    getNextRowRequester: ((Int) -> FocusRequester?)? = null,
    getPrevRowRequester: ((Int) -> FocusRequester?)? = null,
    focusMemoryManager: FocusMemoryManager? = null
) {
    val items = collectionsState.items
    if (items.isEmpty() && !collectionsState.isLoading) return

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val defaultRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Section Header
        Text(
            text = "Collections",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                start = HomeConstants.HOME_CONTENT_START_PADDING,
                bottom = 12.dp
            )
        )

        // Horizontal Carousel
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = HomeConstants.HOME_CONTENT_START_PADDING,
                end = 48.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.id }
            ) { index, collectionItem ->
                val cardRequester = getRequester?.invoke(rowId, index) ?: defaultRequester
                val prevCardRequester = if (index > 0) getRequester?.invoke(rowId, index - 1) else null
                val nextCardRequester = if (index < items.size - 1) getRequester?.invoke(rowId, index + 1) else null

                val upRequester = getPrevRowRequester?.invoke(index)
                val downRequester = getNextRowRequester?.invoke(index)

                CollectionCard(
                    item = collectionItem,
                    onClick = onCollectionClick,
                    focusRequester = cardRequester,
                    onFocus = { focusedItem ->
                        focusMemoryManager?.saveFocus(rowId, index.toString())
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                        onCollectionFocused?.invoke(rowId, focusedItem)
                    },
                    modifier = Modifier.focusProperties {
                        left = if (index == 0) (navFocusRequester ?: left) else (prevCardRequester ?: left)
                        right = nextCardRequester ?: right
                        if (upRequester != null) up = upRequester
                        if (downRequester != null) down = downRequester
                    }
                )
            }
        }
    }
}
