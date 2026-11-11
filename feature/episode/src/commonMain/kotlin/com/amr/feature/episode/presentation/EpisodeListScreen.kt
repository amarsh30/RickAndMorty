package com.amr.feature.episode.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amr.core.designsystem.component.AppEmpty
import com.amr.core.designsystem.component.AppError
import com.amr.core.designsystem.component.AppListCard
import com.amr.core.designsystem.component.AppLoading
import com.amr.core.designsystem.component.PagedLazyColumn
import org.koin.compose.viewmodel.koinViewModel

internal const val EPISODE_LOAD_MORE_INDICATOR_TAG = "episode_load_more_indicator"

@Composable
fun EpisodeListScreen(
    onEpisodeClick: (Int) -> Unit,
    viewModel: EpisodeListViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    EpisodeListContent(
        state = state,
        onRetry = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onEpisodeClick = onEpisodeClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EpisodeListContent(
    state: EpisodeListUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onEpisodeClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Episode", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading || (state.episodes.isEmpty() && state.isRefreshing) ->
                AppLoading(modifier = Modifier.fillMaxSize())

            state.episodes.isEmpty() && state.errorMessage != null ->
                AppError(message = state.errorMessage, onRetry = onRetry, modifier = Modifier.fillMaxSize())

            state.episodes.isEmpty() ->
                AppEmpty(
                    message = "Belum ada episode",
                    actionText = "Muat Episode",
                    onAction = onRetry,
                    modifier = Modifier.fillMaxSize()
                )

            else -> {
                state.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRetry,
                    modifier = Modifier.fillMaxSize()
                ) {
                    PagedLazyColumn(
                        items = state.episodes,
                        key = { it.id },
                        isLoadingMore = state.isLoadingMore,
                        loadMoreErrorMessage = state.loadMoreErrorMessage,
                        isEndReached = state.isEndReached,
                        endReachedText = "Semua episode sudah ditampilkan",
                        onLoadMore = onLoadMore,
                        state = rememberLazyListState(),
                        loadMoreIndicatorTag = EPISODE_LOAD_MORE_INDICATOR_TAG,
                        modifier = Modifier.fillMaxSize()
                    ) { episode ->
                        AppListCard(
                            overline = episode.code,
                            title = episode.name,
                            subtitle = episode.airDate,
                            onClick = { onEpisodeClick(episode.id) }
                        )
                    }
                }
            }
        }
    }
}
