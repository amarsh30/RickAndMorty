package com.amr.feature.episode.detail

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

internal const val EPISODE_CHARACTER_LOAD_MORE_INDICATOR_TAG = "episode_character_load_more_indicator"

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
            TopAppBar(
                title = { Text(state.episode?.code ?: "Detail Episode") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } }
            )
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

            else -> PagedLazyColumn(
                items = state.characters,
                key = { it.id },
                isLoadingMore = state.isLoadingCharacters,
                loadMoreErrorMessage = state.charactersErrorMessage,
                isEndReached = !state.canShowMoreCharacters,
                endReachedText = null,
                onLoadMore = onLoadMoreCharacters,
                loadMoreIndicatorTag = EPISODE_CHARACTER_LOAD_MORE_INDICATOR_TAG,
                contentPadding = PaddingValues(16.dp),
                modifier = contentModifier,
                header = {
                    item(key = "header") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (state.isRefreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            state.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

                            Text(
                                text = episode.name,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.semantics { heading() }
                            )
                            AppDetailRow("Kode", episode.code)
                            AppDetailRow("Tanggal tayang", episode.airDate)

                            Text(
                                text = "Karakter (${state.characterCount})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() }
                            )
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
                AppListCard(
                    title = character.name,
                    subtitle = "${character.status} · ${character.species}",
                    onClick = { onCharacterClick(character.id) }
                )
            }
        }
    }
}
