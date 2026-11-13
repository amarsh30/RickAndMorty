package com.amr.feature.character.favorite

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amr.core.designsystem.component.AppError
import com.amr.core.designsystem.component.AppMessage
import com.amr.core.designsystem.component.AppTopBar
import com.amr.core.designsystem.component.SkeletonList
import com.amr.core.designsystem.icon.AppIcons
import com.amr.feature.character.component.CharacterGrid
import org.koin.compose.viewmodel.koinViewModel

internal const val EXPLORE_CHARACTERS_LABEL = "Jelajahi karakter"
internal const val BACK_TO_CHARACTERS_LABEL = "Kembali ke daftar karakter"

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FavoritesContent(state = state, onBack = onBack, onCharacterClick = onCharacterClick)
}

/**
 * Favorites come from a Room flow, so toggling a favorite anywhere updates this
 * list immediately. [onBack] returns to the character list (Favorites lives in
 * the Karakter tab), which is also where the empty state's CTA leads.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavoritesContent(
    state: FavoritesUiState,
    onBack: () -> Unit,
    onCharacterClick: (Int) -> Unit
) {
    Scaffold(
        topBar = { AppTopBar(title = "Favorit", onBack = onBack) }
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)

        when {
            state.isLoading -> SkeletonList(avatarSize = 80.dp, modifier = contentModifier)

            // Favorites come from a local Room flow with no retry operation, so the button is
            // labelled for what it really does: going back to the character list.
            state.errorMessage != null -> AppError(
                message = state.errorMessage,
                onRetry = onBack,
                retryText = BACK_TO_CHARACTERS_LABEL,
                modifier = contentModifier
            )

            state.favorites.isEmpty() -> AppMessage(
                icon = AppIcons.FavoriteBorder,
                title = "Belum ada karakter favorit",
                description = "Tandai karakter dengan ikon hati di halaman detailnya.",
                actionText = EXPLORE_CHARACTERS_LABEL,
                onAction = onBack,
                modifier = contentModifier
            )

            else -> CharacterGrid(
                characters = state.favorites,
                onCharacterClick = onCharacterClick,
                modifier = contentModifier
            )
        }
    }
}
