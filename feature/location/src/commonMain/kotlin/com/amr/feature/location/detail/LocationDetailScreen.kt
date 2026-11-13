package com.amr.feature.location.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amr.core.designsystem.component.AppError
import com.amr.core.designsystem.component.AppLoading
import com.amr.core.designsystem.component.REFRESHING_LABEL
import com.amr.core.designsystem.component.AppTopBar
import com.amr.core.designsystem.component.AvatarItem
import com.amr.core.designsystem.component.InfoBanner
import com.amr.core.designsystem.component.InfoCard
import com.amr.core.designsystem.component.InfoCardRow
import com.amr.core.designsystem.component.InfoBannerTone
import com.amr.core.designsystem.component.PagedLazyColumn
import com.amr.core.designsystem.component.SectionHeader
import com.amr.core.designsystem.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

internal const val RESIDENT_LOAD_MORE_INDICATOR_TAG = "resident_load_more_indicator"

/** Keeps detail content readable on wide windows (two-pane comes later). */
private val DetailMaxWidth = 720.dp

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

    // Connectivity recovery only while this screen is started (visible).
    LifecycleStartEffect(viewModel) {
        viewModel.setVisible(true)
        onStopOrDispose { viewModel.setVisible(false) }
    }

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
            // The name is the body's heading (and may wrap there), so the bar names the screen type.
            AppTopBar(title = "Detail Lokasi", onBack = onBack)
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

            else -> Box(modifier = contentModifier, contentAlignment = Alignment.TopCenter) {
                // Residents load in windows: the ViewModel decides when more may be fetched,
                // and the footer shows loading / retry for the current window only.
                PagedLazyColumn(
                    items = state.residents,
                    key = { it.id },
                    isLoadingMore = state.isLoadingResidents,
                    loadMoreErrorMessage = state.residentsErrorMessage,
                    isEndReached = !state.canShowMoreResidents,
                    endReachedText = null,
                    onLoadMore = onLoadMoreResidents,
                    loadMoreIndicatorTag = RESIDENT_LOAD_MORE_INDICATOR_TAG,
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier.widthIn(max = DetailMaxWidth).fillMaxSize(),
                    header = {
                        item(key = "header") {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.md)
                            ) {
                                if (state.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = REFRESHING_LABEL }
                )
            }
                                state.errorMessage?.let { InfoBanner(message = it, tone = InfoBannerTone.Warning) }

                                Text(
                                    text = location.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.semantics { heading() }
                                )
                                InfoCard(
                                    rows = listOf(
                                        InfoCardRow("Tipe", location.type),
                                        InfoCardRow("Dimensi", location.dimension)
                                    )
                                )

                                SectionHeader(title = "Penghuni (${state.residentCount})")
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
                    // No image loader in this module: the consistent avatar fallback is used.
                    AvatarItem(
                        title = resident.name,
                        subtitle = "${resident.status} · ${resident.species}",
                        onClick = { onCharacterClick(resident.id) }
                    )
                }
            }
        }
    }
}
