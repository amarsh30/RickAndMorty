package com.amr.feature.episode.presentation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amr.core.designsystem.component.AppEmpty
import com.amr.core.designsystem.component.AppError
import com.amr.core.designsystem.component.AppRefreshAction
import com.amr.core.designsystem.component.AppTopBar
import com.amr.core.designsystem.component.InfoBanner
import com.amr.core.designsystem.component.InfoBannerTone
import com.amr.core.designsystem.component.PagedLazyGrid
import com.amr.core.designsystem.component.SectionHeader
import com.amr.core.designsystem.component.SkeletonList
import com.amr.core.designsystem.component.cardColumnsFor
import com.amr.feature.episode.component.EpisodeCard
import org.koin.compose.viewmodel.koinViewModel

internal const val EPISODE_LOAD_MORE_INDICATOR_TAG = "episode_load_more_indicator"
internal const val EPISODE_GRID_TAG = "episode_grid"
internal const val SEASON_HEADER_TAG = "season_header"

@Composable
fun EpisodeListScreen(
    onEpisodeClick: (Int) -> Unit,
    viewModel: EpisodeListViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Connectivity recovery only while this screen is started (visible).
    LifecycleStartEffect(viewModel) {
        viewModel.setVisible(true)
        onStopOrDispose { viewModel.setVisible(false) }
    }

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
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Episode",
            actions = {
                // Only beside a shown list; empty and error states have their own load button.
                if (!state.isLoading && state.episodes.isNotEmpty()) {
                    AppRefreshAction(isRefreshing = state.isRefreshing, onRefresh = onRetry)
                }
            }
        )

        when {
            // One "loading" node for screen readers; the placeholder rows are hidden.
            state.isLoading || (state.episodes.isEmpty() && state.isRefreshing) ->
                SkeletonList(avatarSize = 48.dp, modifier = Modifier.fillMaxSize())

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
                // Refresh failed but cached episodes are shown.
                state.errorMessage?.let {
                    InfoBanner(
                        message = it,
                        tone = InfoBannerTone.Warning,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Rebuilt from the whole loaded list, so a new page never duplicates a header.
                val rows = remember(state.episodes) { seasonRows(state.episodes) }
                // Hoisted above the width check so resizing keeps the scroll position.
                val gridState = rememberLazyGridState()

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRetry,
                    modifier = Modifier.fillMaxSize()
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        PagedLazyGrid(
                            columns = cardColumnsFor(maxWidth),
                            isLoadingMore = state.isLoadingMore,
                            loadMoreErrorMessage = state.loadMoreErrorMessage,
                            isEndReached = state.isEndReached,
                            endReachedText = "Semua episode sudah ditampilkan",
                            onLoadMore = onLoadMore,
                            state = gridState,
                            loadMoreIndicatorTag = EPISODE_LOAD_MORE_INDICATOR_TAG,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxSize().testTag(EPISODE_GRID_TAG)
                        ) {
                            // Season headers are full-width rows (not sticky): LazyVerticalGrid has no
                            // stable sticky-header API, and this keeps keys and load-more unchanged.
                            items(
                                items = rows,
                                key = { it.key },
                                contentType = { it::class },
                                span = { row ->
                                    if (row is EpisodeListRow.SeasonHeader) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                                }
                            ) { row ->
                                when (row) {
                                    is EpisodeListRow.SeasonHeader ->
                                        SectionHeader(title = row.title, modifier = Modifier.testTag(SEASON_HEADER_TAG))

                                    is EpisodeListRow.Item ->
                                        EpisodeCard(episode = row.episode, onClick = { onEpisodeClick(row.episode.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
