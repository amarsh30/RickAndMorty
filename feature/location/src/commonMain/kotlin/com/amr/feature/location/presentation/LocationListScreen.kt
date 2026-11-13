package com.amr.feature.location.presentation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.amr.core.designsystem.component.SkeletonList
import com.amr.core.designsystem.component.cardColumnsFor
import com.amr.feature.location.component.LocationCard
import org.koin.compose.viewmodel.koinViewModel

internal const val LOCATION_LOAD_MORE_INDICATOR_TAG = "location_load_more_indicator"
internal const val LOCATION_GRID_TAG = "location_grid"

@Composable
fun LocationListScreen(
    onLocationClick: (Int) -> Unit,
    viewModel: LocationListViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Connectivity recovery only while this screen is started (visible).
    LifecycleStartEffect(viewModel) {
        viewModel.setVisible(true)
        onStopOrDispose { viewModel.setVisible(false) }
    }

    LocationListContent(
        state = state,
        onRetry = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onLocationClick = onLocationClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationListContent(
    state: LocationListUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onLocationClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Lokasi",
            actions = {
                // Only beside a shown list; empty and error states have their own load button.
                if (!state.isLoading && state.locations.isNotEmpty()) {
                    AppRefreshAction(isRefreshing = state.isRefreshing, onRefresh = onRetry)
                }
            }
        )

        when {
            // One "loading" node for screen readers; the placeholder rows are hidden.
            state.isLoading || (state.locations.isEmpty() && state.isRefreshing) ->
                SkeletonList(avatarSize = 48.dp, modifier = Modifier.fillMaxSize())

            state.locations.isEmpty() && state.errorMessage != null ->
                AppError(message = state.errorMessage, onRetry = onRetry, modifier = Modifier.fillMaxSize())

            state.locations.isEmpty() ->
                AppEmpty(
                    message = "Belum ada lokasi",
                    actionText = "Muat Lokasi",
                    onAction = onRetry,
                    modifier = Modifier.fillMaxSize()
                )

            else -> {
                // Refresh failed but cached locations are shown.
                state.errorMessage?.let {
                    InfoBanner(
                        message = it,
                        tone = InfoBannerTone.Warning,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Hoisted above the width check so resizing keeps the scroll position.
                val gridState = rememberLazyGridState()

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRetry,
                    modifier = Modifier.fillMaxSize()
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        PagedLazyGrid(
                            items = state.locations,
                            key = { it.id },
                            columns = cardColumnsFor(maxWidth),
                            isLoadingMore = state.isLoadingMore,
                            loadMoreErrorMessage = state.loadMoreErrorMessage,
                            isEndReached = state.isEndReached,
                            endReachedText = "Semua lokasi sudah ditampilkan",
                            onLoadMore = onLoadMore,
                            state = gridState,
                            loadMoreIndicatorTag = LOCATION_LOAD_MORE_INDICATOR_TAG,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxSize().testTag(LOCATION_GRID_TAG)
                        ) { location ->
                            LocationCard(location = location, onClick = { onLocationClick(location.id) })
                        }
                    }
                }
            }
        }
    }
}
