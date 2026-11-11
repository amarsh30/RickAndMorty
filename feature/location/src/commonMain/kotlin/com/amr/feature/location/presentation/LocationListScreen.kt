package com.amr.feature.location.presentation

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
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amr.core.designsystem.component.AppEmpty
import com.amr.core.designsystem.component.AppError
import com.amr.core.designsystem.component.AppListCard
import com.amr.core.designsystem.component.AppLoading
import com.amr.core.designsystem.component.PagedLazyColumn
import org.koin.compose.viewmodel.koinViewModel

internal const val LOCATION_LOAD_MORE_INDICATOR_TAG = "location_load_more_indicator"

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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Lokasi", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading || (state.locations.isEmpty() && state.isRefreshing) ->
                AppLoading(modifier = Modifier.fillMaxSize())

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
                state.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRetry,
                    modifier = Modifier.fillMaxSize()
                ) {
                    PagedLazyColumn(
                        items = state.locations,
                        key = { it.id },
                        isLoadingMore = state.isLoadingMore,
                        loadMoreErrorMessage = state.loadMoreErrorMessage,
                        isEndReached = state.isEndReached,
                        endReachedText = "Semua lokasi sudah ditampilkan",
                        onLoadMore = onLoadMore,
                        state = rememberLazyListState(),
                        loadMoreIndicatorTag = LOCATION_LOAD_MORE_INDICATOR_TAG,
                        modifier = Modifier.fillMaxSize()
                    ) { location ->
                        AppListCard(
                            title = location.name,
                            subtitle = "${location.type} · ${location.dimension}",
                            onClick = { onLocationClick(location.id) }
                        )
                    }
                }
            }
        }
    }
}
