
package com.amr.feature.character.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.amr.core.designsystem.component.InfoBanner
import com.amr.core.designsystem.component.InfoBannerTone
import com.amr.core.designsystem.component.PagedLazyGrid
import com.amr.core.designsystem.component.SkeletonList
import com.amr.core.designsystem.component.AppError
import com.amr.core.designsystem.component.AppEmpty
import com.amr.core.designsystem.component.AppBarAction
import com.amr.core.designsystem.component.AppRefreshAction
import com.amr.core.designsystem.component.AppTopBar
import com.amr.core.designsystem.icon.AppIcons
import com.amr.feature.character.component.AdaptiveCharacterLayout
import com.amr.feature.character.component.CHARACTER_GRID_TAG
import com.amr.feature.character.component.CharacterGridCard

/**
 * Character list. [selectedCharacterId] highlights the card shown in a side
 * detail pane; [gridState] can be hoisted so the scroll position outlives this
 * composable (e.g. while a single-pane detail replaces it).
 */
@Composable
fun CharacterScreen(
    onCharacterClick: (Int) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    viewModel: CharacterViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    selectedCharacterId: Int? = null,
    gridState: LazyGridState = rememberLazyGridState()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Connectivity recovery only while this screen is started (visible).
    LifecycleStartEffect(viewModel) {
        viewModel.setVisible(true)
        onStopOrDispose { viewModel.setVisible(false) }
    }

    CharacterContent(
        state = uiState,
        onRetry = viewModel::refreshCharacters,
        onLoadMore = viewModel::loadMoreCharacters,
        onCharacterClick = onCharacterClick,
        onSearchClick = onSearchClick,
        onFavoritesClick = onFavoritesClick,
        selectedCharacterId = selectedCharacterId,
        gridState = gridState,
        modifier = modifier
    )
}

internal const val CHARACTER_REFRESH_INDICATOR_TAG = "character_refresh_indicator"
internal const val CHARACTER_LOAD_MORE_INDICATOR_TAG = "character_load_more_indicator"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterContent(
    state: CharacterUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    selectedCharacterId: Int?,
    gridState: LazyGridState,
    modifier: Modifier
) {
    // TopAppBar applies the status-bar inset itself when the screen is used standalone.
    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = "Karakter",
            actions = {
                AppBarAction(icon = AppIcons.Search, label = "Cari", onClick = onSearchClick)
                AppBarAction(icon = AppIcons.FavoriteBorder, label = "Favorit", onClick = onFavoritesClick)
                // Only beside a shown list; empty and error states have their own load button.
                if (!state.isLoading && state.characters.isNotEmpty()) {
                    AppRefreshAction(isRefreshing = state.isRefreshing, onRefresh = onRetry)
                }
            }
        )

        when {
            state.isLoading ||
                    (state.characters.isEmpty() && state.isRefreshing) -> {
                // Announced as one "loading" node; the placeholder rows are hidden from screen readers.
                SkeletonList(avatarSize = 80.dp, modifier = Modifier.fillMaxSize())
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
                // Refresh failed but cached data is shown: an inline notice, not a blocking error.
                state.errorMessage?.let { message ->
                    InfoBanner(
                        message = message,
                        tone = InfoBannerTone.Warning,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                    AdaptiveCharacterLayout(modifier = Modifier.fillMaxSize()) { spec ->
                        // Duplicate load-more requests are ignored by the ViewModel.
                        PagedLazyGrid(
                            items = state.characters,
                            key = { it.id },
                            columns = spec.columns,
                            isLoadingMore = state.isLoadingMore,
                            loadMoreErrorMessage = state.loadMoreErrorMessage,
                            isEndReached = state.isEndReached,
                            endReachedText = "Semua karakter sudah ditampilkan",
                            onLoadMore = onLoadMore,
                            state = gridState,
                            loadMoreIndicatorTag = CHARACTER_LOAD_MORE_INDICATOR_TAG,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxSize().testTag(CHARACTER_GRID_TAG)
                        ) { character ->
                            CharacterGridCard(
                                character = character,
                                spec = spec,
                                selected = character.id == selectedCharacterId,
                                onClick = { onCharacterClick(character.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
