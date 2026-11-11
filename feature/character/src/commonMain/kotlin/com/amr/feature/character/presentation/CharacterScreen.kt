
package com.amr.feature.character.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.amr.core.designsystem.component.AppLoading
import com.amr.core.designsystem.component.AppError
import com.amr.core.designsystem.component.AppEmpty
import com.amr.feature.character.component.CharacterCard

@Composable
fun CharacterScreen(
    viewModel: CharacterViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CharacterContent(
        state = uiState,
        onRetry = viewModel::refreshCharacters
    )
}

internal const val CHARACTER_REFRESH_INDICATOR_TAG = "character_refresh_indicator"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterContent(
    state: CharacterUiState,
    onRetry: () -> Unit
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
                    LazyColumn(
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
                    }
                }
            }
        }
    }
}
