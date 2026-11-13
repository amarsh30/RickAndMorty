package com.amr.feature.character.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amr.core.designsystem.theme.AppTheme
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.PaginationState
import com.amr.core.domain.usecase.LoadNextPageCharactersUseCase
import com.amr.core.domain.usecase.ObserveCharactersUseCase
import com.amr.core.domain.usecase.ObservePaginationStateUseCase
import com.amr.core.domain.usecase.RefreshCharactersUseCase
import com.amr.feature.character.component.CHARACTER_GRID_TAG
import com.amr.feature.character.fake.FakeCharacterRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * List-detail container at different window sizes. The detail slot uses a
 * counting ViewModel keyed like the real one, to prove layout changes reuse it.
 */
@OptIn(ExperimentalTestApi::class)
class CharacterListDetailTest {

    private val rick = Character(1, "Rick Sanchez", "Alive", "Human", null)
    private val morty = Character(2, "Morty Smith", "Alive", "Human", null)

    private inner class DetailVm(val id: Int) : ViewModel() {
        override fun onCleared() { cleared += id }
    }

    private var width by mutableStateOf(1000.dp)
    private var height by mutableStateOf(800.dp)
    private var selected by mutableStateOf<Int?>(null)
    private val opened = mutableListOf<Int>()
    private val created = mutableListOf<Int>()
    private val cleared = mutableListOf<Int>()

    private fun listViewModel(characters: List<Character>): CharacterViewModel {
        val repository = FakeCharacterRepository().apply {
            refreshResult = characters
            paginationState.value = PaginationState.EndReached
        }
        return CharacterViewModel(
            observeCharactersUseCase = ObserveCharactersUseCase(repository),
            refreshCharactersUseCase = RefreshCharactersUseCase(repository),
            observePaginationStateUseCase = ObservePaginationStateUseCase(repository),
            loadNextPageCharactersUseCase = LoadNextPageCharactersUseCase(repository),
            connectivityRestored = com.amr.feature.character.fake.connectivityRestored()
        )
    }

    @Composable
    private fun Screen(characters: List<Character> = listOf(rick, morty)) {
        val vm = androidx.compose.runtime.remember { listViewModel(characters) }
        Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(width, height)) {
            AppTheme {
                CharacterListDetailScreen(
                    selectedCharacterId = selected,
                    onSelectedCharacterChange = { selected = it },
                    onOpenCharacter = { opened += it },
                    onSearchClick = {},
                    onFavoritesClick = {},
                    listViewModel = vm,
                    detail = { id, onBack -> FakeDetail(id, onBack) }
                )
            }
        }
    }

    @Composable
    private fun FakeDetail(id: Int, onBack: (() -> Unit)?) {
        // Same keying as the real detail (one ViewModel per character id in this store).
        val vm = viewModel(key = "character-detail-$id") { DetailVm(id).also { created += id } }
        Column {
            Text("Detail ${vm.id}")
            Text(if (onBack == null) "mode: pane" else "mode: full")
            if (onBack != null) Button(onClick = onBack) { Text("Tutup detail") }
        }
    }

    private fun size(w: Dp, h: Dp) { width = w; height = h }

    @Test
    fun compact_isSinglePane_andTapNavigatesLikeBefore() = runComposeUiTest {
        size(400.dp, 800.dp)
        setContent { Screen() }

        onNodeWithTag(CHARACTER_DETAIL_PLACEHOLDER_TAG).assertDoesNotExist()
        onNodeWithText("Rick Sanchez").performClick()

        assertEquals(listOf(1), opened)
        assertNull(selected)
    }

    @Test
    fun mediumTabletPortrait_isSinglePane() = runComposeUiTest {
        size(720.dp, 1200.dp)
        setContent { Screen() }

        onNodeWithTag(CHARACTER_DETAIL_PLACEHOLDER_TAG).assertDoesNotExist()
        onNodeWithText("Morty Smith").performClick()
        assertEquals(listOf(2), opened)
    }

    @Test
    fun expandedButShort_isSinglePane() = runComposeUiTest {
        size(1000.dp, 400.dp)
        setContent { Screen() }

        onNodeWithTag(CHARACTER_DETAIL_PLACEHOLDER_TAG).assertDoesNotExist()
        onNodeWithText("Rick Sanchez").performClick()
        assertEquals(listOf(1), opened)
    }

    @Test
    fun expanded_isTwoPane_selectingAndChangingSelectionUpdatesDetailInPlace() = runComposeUiTest {
        size(1000.dp, 800.dp)
        setContent { Screen() }

        // The simulated 1000dp window is wider than the phone, so the right pane is checked
        // for existence and position (right of the list) rather than on-screen visibility.
        onNodeWithTag(CHARACTER_DETAIL_PLACEHOLDER_TAG).assertExists().assertLeftPositionInRootIsEqualTo(381.dp)
        onNodeWithText("Pilih karakter").assertExists()

        onNodeWithText("Rick Sanchez").performClick()
        onNodeWithTag(CHARACTER_DETAIL_PANE_TAG).assertLeftPositionInRootIsEqualTo(381.dp)
        onNodeWithText("Detail 1").assertExists()
        onNodeWithText("mode: pane").assertExists()
        onNodeWithTag(CHARACTER_DETAIL_PLACEHOLDER_TAG).assertDoesNotExist()
        onNode(hasClickAction() and hasText("Rick Sanchez")).assertIsSelected()

        onNodeWithText("Morty Smith").performClick()
        onNodeWithText("Detail 2").assertExists()
        onNodeWithText("Detail 1").assertDoesNotExist()
        onNode(hasClickAction() and hasText("Morty Smith")).assertIsSelected()

        // Selecting in a pane never navigates.
        assertEquals(emptyList(), opened)
        assertEquals(2, selected)
    }

    @Test
    fun twoPaneToSinglePane_keepsDetailReachable_withSameViewModel_thenCloseReturnsToList() = runComposeUiTest {
        size(1000.dp, 800.dp)
        setContent { Screen() }
        onNodeWithText("Rick Sanchez").performClick()
        onNodeWithText("Detail 1").assertExists()

        size(400.dp, 800.dp)
        waitForIdle()
        onNodeWithText("Detail 1").assertIsDisplayed()
        onNodeWithText("mode: full").assertIsDisplayed()

        // Back to two-pane and again single: still one ViewModel for character 1.
        size(1000.dp, 800.dp)
        waitForIdle()
        size(400.dp, 800.dp)
        waitForIdle()
        assertEquals(listOf(1), created)
        assertEquals(emptyList(), cleared)

        onNodeWithText("Tutup detail").performClick()
        assertNull(selected)
        // Closing the detail releases its ViewModel.
        waitForIdle()
        assertEquals(listOf(1), cleared)
        onNodeWithText("Rick Sanchez").assertIsDisplayed()
        assertEquals(emptyList(), opened)
    }

    @Test
    fun switchingSelection_clearsThePreviousDetail_soViewModelsDoNotPileUp() = runComposeUiTest {
        size(1000.dp, 800.dp)
        setContent { Screen() }

        onNodeWithText("Rick Sanchez").performClick()
        onNodeWithText("Morty Smith").performClick()
        onNodeWithText("Rick Sanchez").performClick()
        waitForIdle()

        // Only the shown detail is alive; each previous one was cleared on the switch.
        assertEquals(listOf(1, 2, 1), created)
        assertEquals(listOf(1, 2), cleared)
        onNodeWithText("Detail 1").assertExists()
    }

    @Test
    fun selecting20CharactersInARow_leavesExactlyOneLiveDetailViewModel() = runComposeUiTest {
        val many = (1..20).map { Character(it, "Character $it", "Alive", "Human", null) }
        size(1000.dp, 800.dp)
        setContent { Screen(many) }

        (1..20).forEach { id ->
            onNodeWithTag(CHARACTER_GRID_TAG).performScrollToIndex(id - 1)
            onNodeWithText("Character $id").performClick()
            onNodeWithText("Detail $id").assertExists()
        }
        waitForIdle()

        assertEquals((1..20).toList(), created)
        assertEquals((1..19).toList(), cleared)
        assertEquals(20, selected)
    }

    @Test
    fun reselectingTheSameCharacter_doesNotRecreateItsViewModel() = runComposeUiTest {
        size(1000.dp, 800.dp)
        setContent { Screen() }

        onNodeWithText("Rick Sanchez").performClick()
        onNodeWithText("Rick Sanchez").performClick()
        waitForIdle()

        assertEquals(listOf(1), created)
        assertEquals(emptyList(), cleared)
    }

    @Test
    fun listScroll_survivesSinglePaneDetailAndResizes() = runComposeUiTest {
        val many = (1..60).map { Character(it, "Character $it", "Alive", "Human", null) }
        size(1000.dp, 800.dp)
        setContent { Screen(many) }

        onNodeWithTag(CHARACTER_GRID_TAG).performScrollToIndex(40)
        onNodeWithText("Character 41").performClick()

        // Shrink: the detail replaces the list...
        size(400.dp, 800.dp)
        waitForIdle()
        onNodeWithText("Detail 41").assertIsDisplayed()

        // ...and closing it shows the list at the same place.
        onNodeWithText("Tutup detail").performClick()
        onNodeWithText("Character 41").assertIsDisplayed()
        onNodeWithText("Character 1").assertDoesNotExist()

        // Grow again: still there, now with the placeholder beside it.
        size(1000.dp, 800.dp)
        waitForIdle()
        onNodeWithText("Character 41").assertIsDisplayed()
        onNodeWithTag(CHARACTER_DETAIL_PLACEHOLDER_TAG).assertIsDisplayed()
    }

    @Test
    fun favoriteToggle_worksInsideThePane_withoutBackArrow() = runComposeUiTest {
        var favorite by mutableStateOf(false)
        size(1000.dp, 800.dp)
        selected = 1
        setContent {
            val vm = androidx.compose.runtime.remember { listViewModel(listOf(rick, morty)) }
            Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(width, height)) {
                AppTheme {
                    CharacterListDetailScreen(
                        selectedCharacterId = selected,
                        onSelectedCharacterChange = { selected = it },
                        onOpenCharacter = {},
                        onSearchClick = {},
                        onFavoritesClick = {},
                        listViewModel = vm,
                        detail = { _, onBack ->
                            com.amr.feature.character.detail.CharacterDetailContent(
                                state = com.amr.feature.character.detail.CharacterDetailUiState(
                                    isLoading = false, character = rick, isFavorite = favorite
                                ),
                                onBack = onBack ?: {},
                                onRetry = {},
                                onToggleFavorite = { favorite = !favorite },
                                showBackButton = onBack != null
                            )
                        }
                    )
                }
            }
        }

        onNodeWithTag(CHARACTER_DETAIL_PANE_TAG).assertExists()
        onNodeWithContentDescription("Kembali").assertDoesNotExist()
        // Its centre is beyond the phone's edge, so trigger the click action directly.
        onNodeWithText("Tambah ke Favorit").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Hapus dari Favorit").assertExists()
        assertEquals(true, favorite)
    }

    @Test
    fun detailPane_titleNamesTheSelectedCharacter_andChangesWithTheSelection() = runComposeUiTest {
        size(1000.dp, 800.dp)
        setContent { Screen() }

        onNodeWithText("Rick Sanchez").performClick()
        onNodeWithTag(CHARACTER_DETAIL_PANE_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, characterDetailPaneTitle("Rick Sanchez")))

        // A new selection gives a new title (announced once); selection and in-place detail are kept.
        onNodeWithText("Morty Smith").performClick()
        onNodeWithTag(CHARACTER_DETAIL_PANE_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, characterDetailPaneTitle("Morty Smith")))
        onNode(hasClickAction() and hasText("Morty Smith")).assertIsSelected()
        onNodeWithText("Detail 2").assertExists()
        // Exactly one pane title on screen: the list itself is not a pane.
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle)).assertCountEquals(1)
    }

    @Test
    fun detailPaneTitle_fallsBackToGenericTitle_whenTheNameIsUnknown() {
        assertEquals(CHARACTER_DETAIL_PANE_TITLE, characterDetailPaneTitle(null))
        assertEquals(CHARACTER_DETAIL_PANE_TITLE, characterDetailPaneTitle(" "))
        assertEquals("$CHARACTER_DETAIL_PANE_TITLE: Rick Sanchez", characterDetailPaneTitle("Rick Sanchez"))
    }

    @Test
    fun singlePaneToTwoPane_withoutSelection_showsPlaceholder_andDoesNotNavigate() = runComposeUiTest {
        size(400.dp, 800.dp)
        setContent { Screen() }

        size(1000.dp, 800.dp)
        waitForIdle()

        onNodeWithTag(CHARACTER_DETAIL_PLACEHOLDER_TAG).assertIsDisplayed()
        assertEquals(emptyList(), opened)
        assertEquals(emptyList(), created)
    }
}
