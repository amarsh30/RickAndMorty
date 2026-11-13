package com.amr.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

const val PAGED_LIST_LOAD_MORE_INDICATOR_TAG = "paged_list_load_more_indicator"
const val LOAD_MORE_LOADING_LABEL = "Memuat data berikutnya"
const val LOAD_MORE_RETRY_LABEL = "Muat Lagi"

/** Load the next page when the last visible item is within this many items of the end. */
internal const val LOAD_MORE_THRESHOLD = 5

/**
 * A list that asks for more items when its end comes into view and shows the
 * load-more state in a footer. Rendering only: the caller decides whether a
 * request is allowed and ignores duplicate [onLoadMore] calls.
 */
@Composable
fun <T> PagedLazyColumn(
    items: List<T>,
    key: (T) -> Any,
    isLoadingMore: Boolean,
    loadMoreErrorMessage: String?,
    isEndReached: Boolean,
    endReachedText: String?,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    loadMoreIndicatorTag: String = PAGED_LIST_LOAD_MORE_INDICATOR_TAG,
    header: LazyListScope.() -> Unit = {},
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    LoadMoreWhenNearEnd(listState = state, onLoadMore = onLoadMore)

    LazyColumn(
        state = state,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = contentPadding
    ) {
        header()

        items(items = items, key = key, itemContent = itemContent)

        item(key = "load_more_footer") {
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

/**
 * Requests more when the end of the list comes into view. Keyed on the item
 * count too, so a page that still leaves the end visible (e.g. on a tablet)
 * triggers the following one, while a failed load does not retry by itself.
 */
@Composable
private fun LoadMoreWhenNearEnd(
    listState: LazyListState,
    onLoadMore: () -> Unit
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = layoutInfo.totalItemsCount
            (total > 0 && lastVisible >= total - 1 - LOAD_MORE_THRESHOLD) to total
        }
            .distinctUntilChanged()
            .filter { (isNearEnd, _) -> isNearEnd }
            .collect { currentOnLoadMore() }
    }
}

@Composable
internal fun LoadMoreFooter(
    isLoadingMore: Boolean,
    loadMoreErrorMessage: String?,
    isEndReached: Boolean,
    endReachedText: String?,
    loadMoreIndicatorTag: String,
    onLoadMore: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoadingMore -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                        .testTag(loadMoreIndicatorTag)
                        .semantics { contentDescription = LOAD_MORE_LOADING_LABEL }
                )
            }

            loadMoreErrorMessage != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Polite: the failure happens off-screen from the user's focus, at the list end.
                    Text(
                        text = loadMoreErrorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    TextButton(onClick = onLoadMore, modifier = Modifier.focusRing(ButtonDefaults.textShape)) {
                        Text(LOAD_MORE_RETRY_LABEL)
                    }
                }
            }

            // Not a live region: reaching the end is expected and is read when the user gets there.
            isEndReached && endReachedText != null -> {
                Text(
                    text = endReachedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
