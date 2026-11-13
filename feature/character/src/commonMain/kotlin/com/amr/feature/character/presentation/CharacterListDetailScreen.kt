package com.amr.feature.character.presentation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import com.amr.core.designsystem.component.AppMessage
import com.amr.core.designsystem.icon.AppIcons
import com.amr.feature.character.component.CharacterListPaneWidth
import com.amr.feature.character.component.CharacterPaneLayout
import com.amr.feature.character.component.characterPaneLayout
import org.koin.compose.viewmodel.koinViewModel

internal const val CHARACTER_LIST_PANE_TAG = "character_list_pane"
internal const val CHARACTER_DETAIL_PANE_TAG = "character_detail_pane"
internal const val CHARACTER_DETAIL_PLACEHOLDER_TAG = "character_detail_placeholder"
private const val DETAIL_PANE_STORE_KEY = "character-detail-pane-store"
internal const val CHARACTER_DETAIL_PANE_TITLE = "Detail karakter"

/**
 * Pane title for the shown detail. It names the character when the list knows it, so the
 * title changes on every new selection and screen readers announce which detail opened.
 */
internal fun characterDetailPaneTitle(name: String?) =
    if (name.isNullOrBlank()) CHARACTER_DETAIL_PANE_TITLE else "$CHARACTER_DETAIL_PANE_TITLE: $name"

/**
 * Character list with an adaptive detail, for the list destination.
 *
 * - Two-pane (enough width *and* height): taps select a character, whose detail
 *   shows beside the list. The caller owns [selectedCharacterId] (saved in the
 *   destination's saved state) and the system Back that clears it.
 * - Single-pane: taps call [onOpenCharacter] (the existing navigation to the
 *   detail route). If a selection exists because the window shrank from
 *   two-pane, that detail is shown here full-screen instead, and Back/← clears it.
 *
 * The shown detail's ViewModel lives in a store owned by [DetailPaneStoreHolder]:
 * switching between the two modes (or rotating) reuses it, and selecting another
 * character clears the previous one, so ViewModels never pile up. [detail]
 * renders it; its `onBack` is null when shown as a pane.
 */
@Composable
fun CharacterListDetailScreen(
    selectedCharacterId: Int?,
    onSelectedCharacterChange: (Int?) -> Unit,
    onOpenCharacter: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    detail: @Composable (characterId: Int, onBack: (() -> Unit)?) -> Unit,
    modifier: Modifier = Modifier,
    listViewModel: CharacterViewModel = koinViewModel()
) {
    // Outlives the list composable, which is swapped out while a single-pane detail shows.
    val gridState = rememberLazyGridState()
    val listState by listViewModel.uiState.collectAsStateWithLifecycle()

    // One ViewModelStore for the shown detail only (see DetailPaneStoreHolder): the same
    // character keeps its ViewModel across rotation/resize; a new one replaces the old.
    val paneStores = viewModel(key = DETAIL_PANE_STORE_KEY) { DetailPaneStoreHolder() }
    val activeOwner = selectedCharacterId?.let { paneStores.ownerFor(it) }
    // Only after this composition is applied: make its detail's store the active one.
    SideEffect { paneStores.commit(selectedCharacterId) }
    val paneDetail: @Composable (Int, (() -> Unit)?) -> Unit = { id, onBack ->
        CompositionLocalProvider(LocalViewModelStoreOwner provides checkNotNull(activeOwner)) {
            detail(id, onBack)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layout = characterPaneLayout(maxWidth, maxHeight)
        val current = selectedCharacterId

        when {
            layout == CharacterPaneLayout.TwoPane -> Row(modifier = Modifier.fillMaxSize()) {
                CharacterScreen(
                    onCharacterClick = { onSelectedCharacterChange(it) },
                    onSearchClick = onSearchClick,
                    onFavoritesClick = onFavoritesClick,
                    viewModel = listViewModel,
                    selectedCharacterId = current,
                    gridState = gridState,
                    modifier = Modifier.width(CharacterListPaneWidth).fillMaxHeight().testTag(CHARACTER_LIST_PANE_TAG)
                )
                VerticalDivider()
                if (current != null) {
                    // A pane title that names the character makes screen readers announce each new
                    // selection once, instead of the right side updating silently. Focus is left on
                    // the list on purpose, so browsing and comparing characters stays uninterrupted.
                    val paneTitleText = characterDetailPaneTitle(listState.characters.firstOrNull { it.id == current }?.name)
                    BoxWithConstraints(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag(CHARACTER_DETAIL_PANE_TAG)
                            .semantics { paneTitle = paneTitleText }
                    ) {
                        paneDetail(current, null)
                    }
                } else {
                    AppMessage(
                        icon = AppIcons.Person,
                        title = "Pilih karakter",
                        description = "Detail karakter yang dipilih akan tampil di sini.",
                        modifier = Modifier.weight(1f).fillMaxHeight().testTag(CHARACTER_DETAIL_PLACEHOLDER_TAG)
                    )
                }
            }

            // Shrunk from two-pane with a selection: keep that detail reachable, same ViewModel.
            current != null -> paneDetail(current) { onSelectedCharacterChange(null) }

            else -> CharacterScreen(
                onCharacterClick = onOpenCharacter,
                onSearchClick = onSearchClick,
                onFavoritesClick = onFavoritesClick,
                viewModel = listViewModel,
                gridState = gridState,
                modifier = Modifier.testTag(CHARACTER_LIST_PANE_TAG)
            )
        }
    }
}
