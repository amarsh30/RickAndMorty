
package com.amr.feature.character.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.amr.core.designsystem.component.AppLoading
import com.amr.core.designsystem.component.AppError
import com.amr.core.designsystem.component.AppEmpty
import com.amr.feature.character.component.CharacterCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun CharacterScreen(
    viewModel: CharacterViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CharacterContent(
        state = uiState,
        onRetry = viewModel::refreshCharacters,
        onLoadMore = viewModel::loadMoreCharacters
    )
}

internal const val CHARACTER_REFRESH_INDICATOR_TAG = "character_refresh_indicator"
internal const val CHARACTER_LOAD_MORE_INDICATOR_TAG = "character_load_more_indicator"

/** Load the next page when the last visible item is within this many items of the end. */
private const val LOAD_MORE_THRESHOLD = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterContent(
    state: CharacterUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Rick and Morty Characters",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading ||
                    (state.characters.isEmpty() && state.isRefreshing) -> {
                AppLoading(
                    modifier = Modifier.fillMaxSize()
                )
            }

            state.characters.isEmpty() &&
                    state.errorMessage != null -> {
                AppError(
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize()
                )
            }

            state.characters.isEmpty() -> {
                AppEmpty(
                    message = "Belum ada karakter",
                    actionText = "Muat Karakter",
                    onAction = onRetry,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // The pull-to-refresh indicator is the only refresh indicator for the list;
                // its visibility is driven by the ViewModel's isRefreshing.
                val pullToRefreshState = rememberPullToRefreshState()

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRetry,
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullToRefreshState,
                            isRefreshing = state.isRefreshing,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .testTag(CHARACTER_REFRESH_INDICATOR_TAG)
                        )
                    }
                ) {
                    val listState = rememberLazyListState()

                    LoadMoreWhenNearEnd(listState = listState, onLoadMore = onLoadMore)

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = state.characters,
                            key = { it.id }
                        ) { character ->
                            CharacterCard(
                                name = character.name,
                                status = character.status,
                                imageUrl = character.imageUrl
                            )
                        }

                        item(key = "load_more_footer") {
                            LoadMoreFooter(state = state, onLoadMore = onLoadMore)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Requests the next page when the end of the list comes into view. Keyed on the
 * item count too, so a page that still leaves the end visible (e.g. on a tablet)
 * triggers the following one, while a failed load does not retry by itself.
 * Duplicate calls are ignored by the ViewModel.
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
private fun LoadMoreFooter(
    state: CharacterUiState,
    onLoadMore: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isLoadingMore -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(32.dp)
                        .testTag(CHARACTER_LOAD_MORE_INDICATOR_TAG)
                )
            }

            state.loadMoreErrorMessage != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.loadMoreErrorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = onLoadMore) {
                        Text("Muat Lagi")
                    }
                }
            }

            state.isEndReached -> {
                Text(
                    text = "Semua karakter sudah ditampilkan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
