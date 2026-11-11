package com.amr.feature.location.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amr.core.designsystem.component.AppDetailRow
import com.amr.core.designsystem.component.AppError
import com.amr.core.designsystem.component.AppListCard
import com.amr.core.designsystem.component.AppLoading
import com.amr.core.designsystem.component.PagedLazyColumn
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

internal const val RESIDENT_LOAD_MORE_INDICATOR_TAG = "resident_load_more_indicator"

@Composable
fun LocationDetailScreen(
    locationId: Int,
    onBack: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    viewModel: LocationDetailViewModel = koinViewModel(
        key = "location-detail-$locationId",
        parameters = { parametersOf(locationId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LocationDetailContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onLoadMoreResidents = viewModel::loadMoreResidents,
        onCharacterClick = onCharacterClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationDetailContent(
    state: LocationDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoadMoreResidents: () -> Unit,
    onCharacterClick: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.location?.name ?: "Detail Lokasi") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } }
            )
        }
    ) { padding ->
        val location = state.location
        val contentModifier = Modifier.fillMaxSize().padding(padding)

        when {
            state.isLoading -> AppLoading(modifier = contentModifier)

            location == null -> AppError(
                message = state.errorMessage ?: "Data lokasi tidak ditemukan.",
                onRetry = onRetry,
                modifier = contentModifier
            )

            else -> PagedLazyColumn(
                items = state.residents,
                key = { it.id },
                isLoadingMore = state.isLoadingResidents,
                loadMoreErrorMessage = state.residentsErrorMessage,
                isEndReached = !state.canShowMoreResidents,
                endReachedText = null,
                onLoadMore = onLoadMoreResidents,
                loadMoreIndicatorTag = RESIDENT_LOAD_MORE_INDICATOR_TAG,
                contentPadding = PaddingValues(16.dp),
                modifier = contentModifier,
                header = {
                    item(key = "header") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (state.isRefreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            state.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

                            Text(
                                text = location.name,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.semantics { heading() }
                            )
                            AppDetailRow("Tipe", location.type)
                            AppDetailRow("Dimensi", location.dimension)

                            Text(
                                text = "Penghuni (${state.residentCount})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() }
                            )
                            if (state.residentCount == 0) {
                                Text(
                                    text = "Tidak ada penghuni yang diketahui.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            ) { resident ->
                AppListCard(
                    title = resident.name,
                    subtitle = "${resident.status} · ${resident.species}",
                    onClick = { onCharacterClick(resident.id) }
                )
            }
        }
    }
}
