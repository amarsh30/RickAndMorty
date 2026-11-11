package com.amr.feature.character.favorite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amr.core.designsystem.component.AppEmpty
import com.amr.core.designsystem.component.AppError
import com.amr.core.designsystem.component.AppLoading
import com.amr.feature.character.component.CharacterCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FavoritesContent(state = state, onBack = onBack, onCharacterClick = onCharacterClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavoritesContent(
    state: FavoritesUiState,
    onBack: () -> Unit,
    onCharacterClick: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorit") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } }
            )
        }
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)

        when {
            state.isLoading -> AppLoading(modifier = contentModifier)

            state.errorMessage != null -> AppError(
                message = state.errorMessage,
                onRetry = onBack,
                modifier = contentModifier
            )

            state.favorites.isEmpty() -> AppEmpty(
                message = "Belum ada karakter favorit",
                actionText = "Kembali",
                onAction = onBack,
                modifier = contentModifier
            )

            else -> LazyColumn(
                modifier = contentModifier,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(items = state.favorites, key = { it.id }) { character ->
                    CharacterCard(
                        name = character.name,
                        status = character.status,
                        imageUrl = character.imageUrl,
                        onClick = { onCharacterClick(character.id) }
                    )
                }
            }
        }
    }
}
