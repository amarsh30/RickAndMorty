package com.amr.feature.episode.detail

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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amr.core.designsystem.component.AppError
import com.amr.core.designsystem.component.AppLoading
import com.amr.core.designsystem.component.AppTopBar
import com.amr.core.designsystem.component.AvatarItem
import com.amr.core.designsystem.component.InfoBanner
import com.amr.core.designsystem.component.InfoBannerTone
import com.amr.core.designsystem.component.InfoCard
import com.amr.core.designsystem.component.InfoCardRow
import com.amr.core.designsystem.component.PagedLazyColumn
import com.amr.core.designsystem.component.SectionHeader
import com.amr.core.designsystem.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

internal const val EPISODE_CHARACTER_LOAD_MORE_INDICATOR_TAG = "episode_character_load_more_indicator"

/** Keeps detail content readable on wide windows (two-pane comes later). */
private val DetailMaxWidth = 720.dp

@Composable
fun EpisodeDetailScreen(
    episodeId: Int,
    onBack: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    viewModel: EpisodeDetailViewModel = koinViewModel(
        key = "episode-detail-$episodeId",
        parameters = { parametersOf(episodeId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Connectivity recovery only while this screen is started (visible).
    LifecycleStartEffect(viewModel) {
        viewModel.setVisible(true)
        onStopOrDispose { viewModel.setVisible(false) }
    }

    EpisodeDetailContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onLoadMoreCharacters = viewModel::loadMoreCharacters,
        onCharacterClick = onCharacterClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EpisodeDetailContent(
    state: EpisodeDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoadMoreCharacters: () -> Unit,
    onCharacterClick: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            // The title is the body's heading (and may wrap there), so the bar names the screen type.
            AppTopBar(title = "Detail Episode", onBack = onBack)
        }
    ) { padding ->
        val episode = state.episode
        val contentModifier = Modifier.fillMaxSize().padding(padding)

        when {
            state.isLoading -> AppLoading(modifier = contentModifier)

            episode == null -> AppError(
                message = state.errorMessage ?: "Data episode tidak ditemukan.",
                onRetry = onRetry,
                modifier = contentModifier
            )

            else -> Box(modifier = contentModifier, contentAlignment = Alignment.TopCenter) {
                // Characters load in windows: the ViewModel decides when more may be fetched,
                // and the footer shows loading / retry for the current window only.
                PagedLazyColumn(
                    items = state.characters,
                    key = { it.id },
                    isLoadingMore = state.isLoadingCharacters,
                    loadMoreErrorMessage = state.charactersErrorMessage,
                    isEndReached = !state.canShowMoreCharacters,
                    endReachedText = null,
                    onLoadMore = onLoadMoreCharacters,
                    loadMoreIndicatorTag = EPISODE_CHARACTER_LOAD_MORE_INDICATOR_TAG,
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier.widthIn(max = DetailMaxWidth).fillMaxSize(),
                    header = {
                        item(key = "header") {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.md)
                            ) {
                                if (state.isRefreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                state.errorMessage?.let { InfoBanner(message = it, tone = InfoBannerTone.Warning) }

                                Text(
                                    text = episode.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.semantics { heading() }
                                )
                                InfoCard(
                                    rows = listOf(
                                        InfoCardRow("Kode", episode.code, monospace = true),
                                        InfoCardRow("Tanggal tayang", episode.airDate)
                                    )
                                )

                                SectionHeader(title = "Karakter (${state.characterCount})")
                                if (state.characterCount == 0) {
                                    Text(
                                        text = "Tidak ada karakter yang diketahui.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                ) { character ->
                    // No image loader in this module: the consistent avatar fallback is used.
                    AvatarItem(
                        title = character.name,
                        subtitle = "${character.status} · ${character.species}",
                        onClick = { onCharacterClick(character.id) }
                    )
                }
            }
        }
    }
}
