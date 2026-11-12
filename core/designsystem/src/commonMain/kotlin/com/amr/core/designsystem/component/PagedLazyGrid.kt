package com.amr.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Grid counterpart of [PagedLazyColumn]: same load-more rule and footer, with
 * the footer spanning the full row. Using one grid for every width (a single
 * column on compact screens) keeps one [LazyGridState], so the scroll
 * position survives window resizes.
 */
@Composable
fun <T> PagedLazyGrid(
    items: List<T>,
    key: (T) -> Any,
    columns: GridCells,
    isLoadingMore: Boolean,
    loadMoreErrorMessage: String?,
    isEndReached: Boolean,
    endReachedText: String?,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    loadMoreIndicatorTag: String = PAGED_LIST_LOAD_MORE_INDICATOR_TAG,
    itemContent: @Composable LazyGridItemScope.(T) -> Unit
) {
    PagedLazyGrid(
        columns = columns,
        isLoadingMore = isLoadingMore,
        loadMoreErrorMessage = loadMoreErrorMessage,
        isEndReached = isEndReached,
        endReachedText = endReachedText,
        onLoadMore = onLoadMore,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        loadMoreIndicatorTag = loadMoreIndicatorTag
    ) {
        items(items = items, key = key, itemContent = itemContent)
    }
}

/**
 * Free-form variant for grids with extra rows (section or detail headers): [content]
 * emits the items, then the shared load-more footer is appended. Every item in
 * [content] needs a stable key.
 */
@Composable
fun PagedLazyGrid(
    columns: GridCells,
    isLoadingMore: Boolean,
    loadMoreErrorMessage: String?,
    isEndReached: Boolean,
    endReachedText: String?,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    loadMoreIndicatorTag: String = PAGED_LIST_LOAD_MORE_INDICATOR_TAG,
    content: LazyGridScope.() -> Unit
) {
    LoadMoreWhenGridNearEnd(gridState = state, onLoadMore = onLoadMore)

    LazyVerticalGrid(
        columns = columns,
        state = state,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = contentPadding
    ) {
        content()

        item(key = "load_more_footer", span = { GridItemSpan(maxLineSpan) }) {
            LoadMoreFooter(
                isLoadingMore = isLoadingMore,
                loadMoreErrorMessage = loadMoreErrorMessage,
                isEndReached = isEndReached,
                endReachedText = endReachedText,
                loadMoreIndicatorTag = loadMoreIndicatorTag,
                onLoadMore = onLoadMore
            )
        }
    }
}

/** Below this available width, card lists use a single column. */
private val CompactListMaxWidth = 600.dp

/**
 * Columns for text-first cards (no large images): one column on compact widths,
 * otherwise as many [minCardWidth]-wide columns as fit, so cards never stretch
 * across a whole tablet.
 */
fun cardColumnsFor(availableWidth: Dp, minCardWidth: Dp = 280.dp): GridCells =
    if (availableWidth < CompactListMaxWidth) GridCells.Fixed(1) else GridCells.Adaptive(minCardWidth)

/** Same rule as the list version: near the end, re-armed when the item count changes. */
@Composable
private fun LoadMoreWhenGridNearEnd(gridState: LazyGridState, onLoadMore: () -> Unit) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(gridState) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = layoutInfo.totalItemsCount
            (total > 0 && lastVisible >= total - 1 - LOAD_MORE_THRESHOLD) to total
        }
            .distinctUntilChanged()
            .filter { (isNearEnd, _) -> isNearEnd }
            .collect { currentOnLoadMore() }
    }
}
