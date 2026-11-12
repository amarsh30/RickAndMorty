package com.amr.feature.character.presentation

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.component.INFO_BANNER_TAG
import com.amr.core.designsystem.component.SKELETON_LIST_TAG
import com.amr.core.designsystem.component.STATUS_CHIP_TAG
import com.amr.core.designsystem.theme.AppTheme
import com.amr.feature.character.component.CHARACTER_GRID_TAG
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.PaginationState
import com.amr.core.domain.usecase.LoadNextPageCharactersUseCase
import com.amr.core.domain.usecase.ObserveCharactersUseCase
import com.amr.core.domain.usecase.ObservePaginationStateUseCase
import com.amr.core.domain.usecase.RefreshCharactersUseCase
import com.amr.feature.character.fake.FakeCharacterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * UI test untuk [CharacterScreen] — layar benar-benar digambar di emulator.
 *
 * ViewModel asli dipakai, hanya repository yang diganti fake, sehingga test
 * tidak butuh internet/Koin dan hasilnya selalu sama.
 *
 * Jalankan: ./gradlew :feature:character:connectedAndroidDeviceTest
 */
@OptIn(ExperimentalTestApi::class)
class CharacterScreenTest {

    private val rick = Character(1, "Rick Sanchez", "Alive", "Human", null)
    private val morty = Character(2, "Morty Smith", "Alive", "Human", null)

    private fun createViewModel(repository: FakeCharacterRepository) = CharacterViewModel(
        observeCharactersUseCase = ObserveCharactersUseCase(repository),
        refreshCharactersUseCase = RefreshCharactersUseCase(repository),
        observePaginationStateUseCase = ObservePaginationStateUseCase(repository),
        loadNextPageCharactersUseCase = LoadNextPageCharactersUseCase(repository),
        connectivityRestored = com.amr.feature.character.fake.connectivityRestored()
    )

    /** Pull-to-refresh indicator in its refreshing (spinning) state. */
    private val refreshingIndicator: SemanticsMatcher =
        hasTestTag(CHARACTER_REFRESH_INDICATOR_TAG) and
            hasAnyDescendant(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))

    private val anyIndeterminateProgress: SemanticsMatcher =
        hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)

    // --- Infinite scroll (E6) ---

    private fun manyCharacters(count: Int) =
        (1..count).map { Character(it, "Character $it", "Alive", "Human", null) }

    @Test
    fun shortListWithMorePages_loadsNextPageAndShowsFooterIndicator() = runComposeUiTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeCharacterRepository().apply {
            refreshResult = listOf(rick)
            paginationState.value = PaginationState.HasMore
            loadNextPageGate = gate
            nextPageResult = listOf(morty)
            paginationStateAfterLoad = PaginationState.EndReached
        }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }

        // The end of a one-item list is visible, so the next page is requested right away.
        onNodeWithTag(CHARACTER_LOAD_MORE_INDICATOR_TAG).assertExists()
        assertEquals(1, repository.loadNextPageCount)

        gate.complete(Unit)

        onNodeWithText("Morty Smith").assertIsDisplayed()
        onNodeWithTag(CHARACTER_LOAD_MORE_INDICATOR_TAG).assertDoesNotExist()
        onNodeWithText("Semua karakter sudah ditampilkan").assertIsDisplayed()
    }

    @Test
    fun loadMoreFails_showsFooterRetryThatLoadsNextPage() = runComposeUiTest {
        val repository = FakeCharacterRepository().apply {
            refreshResult = listOf(rick)
            paginationState.value = PaginationState.HasMore
            loadNextPageError = DataError.NoConnection()
        }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }

        onNodeWithText("Gagal memuat karakter. Periksa koneksi internet.").assertIsDisplayed()
        onNodeWithText("Rick Sanchez").assertIsDisplayed()
        // A failed load is not retried automatically.
        assertEquals(1, repository.loadNextPageCount)

        repository.loadNextPageError = null
        repository.nextPageResult = listOf(morty)
        // Last page, so the grown list does not trigger another automatic load.
        repository.paginationStateAfterLoad = PaginationState.EndReached
        onNodeWithText("Muat Lagi").performClick()

        onNodeWithText("Morty Smith").assertIsDisplayed()
        assertEquals(2, repository.loadNextPageCount)
    }

    @Test
    fun endReached_showsEndOfListMessageWithoutLoading() = runComposeUiTest {
        val repository = FakeCharacterRepository().apply {
            refreshResult = listOf(rick)
            paginationState.value = PaginationState.EndReached
        }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }

        onNodeWithText("Semua karakter sudah ditampilkan").assertIsDisplayed()
        assertEquals(0, repository.loadNextPageCount)
    }

    @Test
    fun scrollingToEnd_triggersLoadMoreOnlyNearTheEnd() = runComposeUiTest {
        val characters = manyCharacters(40)
        val repository = FakeCharacterRepository().apply {
            refreshResult = characters
            paginationState.value = PaginationState.HasMore
        }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }
        waitForIdle()
        assertEquals(0, repository.loadNextPageCount)

        onNode(hasScrollAction()).performScrollToIndex(characters.lastIndex)
        waitForIdle()

        assertEquals(1, repository.loadNextPageCount)
    }

    @Test
    fun whenRefreshSucceeds_showsCharacterList() = runComposeUiTest {
        val repository = FakeCharacterRepository().apply {
            refreshResult = listOf(rick, morty)
        }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }

        onNodeWithText("Karakter").assertIsDisplayed()
        onNodeWithText("Rick Sanchez").assertIsDisplayed()
        onNodeWithText("Morty Smith").assertIsDisplayed()
    }

    @Test
    fun card_showsNameSpeciesAndStatusChip() = runComposeUiTest {
        val dead = Character(3, "Birdperson", "Dead", "Bird-Person", null)
        val repository = FakeCharacterRepository().apply { refreshResult = listOf(rick, dead) }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }

        onNodeWithText("Birdperson").assertIsDisplayed()
        onNodeWithText("Bird-Person").assertIsDisplayed()
        onAllNodesWithTag(STATUS_CHIP_TAG).assertCountEquals(2)
        onNode(hasTestTag(STATUS_CHIP_TAG) and hasText("Dead")).assertIsDisplayed()
        // The card is one clickable node with a 48dp+ target.
        onNode(hasClickAction() and hasText("Birdperson")).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun initialLoading_showsSkeletonAsSingleLoadingNode() = runComposeUiTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeCharacterRepository().apply {
            this.gate = gate
            refreshResult = listOf(rick)
        }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }

        onNodeWithTag(SKELETON_LIST_TAG).assertIsDisplayed()
        onNodeWithContentDescription("Memuat").assertIsDisplayed()
        // No placeholder card is exposed as content.
        onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag(SKELETON_LIST_TAG))).assertCountEquals(0)

        gate.complete(Unit)
        onNodeWithTag(SKELETON_LIST_TAG).assertDoesNotExist()
        onNodeWithText("Rick Sanchez").assertIsDisplayed()
    }

    @Test
    fun refreshFailureWithCache_isAnInlineBannerAboveTheList() = runComposeUiTest {
        val repository = FakeCharacterRepository().apply {
            characters.value = listOf(rick)
            errorToThrow = DataError.NoConnection()
        }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }

        onNode(hasTestTag(INFO_BANNER_TAG) and hasAnyDescendant(hasText("Gagal memperbarui data. Menampilkan data tersimpan.")))
            .assertIsDisplayed()
        onNodeWithText("Rick Sanchez").assertIsDisplayed()
    }

    @Test
    fun compactWidth_isSingleColumn_wideWidth_isMultiColumnGrid() = runComposeUiTest {
        var width by mutableStateOf(400.dp)
        val repository = FakeCharacterRepository().apply { refreshResult = listOf(rick, morty) }
        val viewModel = createViewModel(repository)

        setContent { WidthBox(width) { CharacterScreen(viewModel = viewModel) } }

        val rickTop = { onNodeWithText("Rick Sanchez").getUnclippedBoundsInRoot().top }
        val mortyTop = { onNodeWithText("Morty Smith").getUnclippedBoundsInRoot().top }
        assertTrue(mortyTop() > rickTop(), "compact: Morty is below Rick")

        width = 900.dp
        waitForIdle()
        assertEquals(rickTop(), mortyTop(), "wide: Rick and Morty share a row")
    }

    @Test
    fun resizing_keepsScrollPosition() = runComposeUiTest {
        var width by mutableStateOf(400.dp)
        val characters = manyCharacters(60)
        val repository = FakeCharacterRepository().apply {
            refreshResult = characters
            paginationState.value = PaginationState.EndReached
        }
        val viewModel = createViewModel(repository)

        setContent { WidthBox(width) { CharacterScreen(viewModel = viewModel) } }

        onNodeWithTag(CHARACTER_GRID_TAG).performScrollToIndex(40)
        onNodeWithText("Character 41").assertIsDisplayed()

        width = 900.dp
        waitForIdle()
        onNodeWithText("Character 41").assertIsDisplayed()
        onNodeWithText("Character 1").assertDoesNotExist()
    }

    @Test
    fun largeFontScale_keepsNameAndStatusReadable() = runComposeUiTest {
        val longName = Character(4, "Abradolf Lincler the Second", "unknown", "Human", null)
        val repository = FakeCharacterRepository().apply { refreshResult = listOf(longName) }

        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                WidthBox(360.dp) { CharacterScreen(viewModel = createViewModel(repository)) }
            }
        }

        // Name wraps to two lines instead of being cut off, and the status stays visible.
        onNodeWithText("Abradolf Lincler the Second").assertIsDisplayed()
        assertFalse(textOverflows("Abradolf Lincler the Second"))
        onNode(hasTestTag(STATUS_CHIP_TAG) and hasText("unknown")).assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionsProvider.textOverflows(text: String): Boolean {
        val results = mutableListOf<TextLayoutResult>()
        onNodeWithText(text).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first().hasVisualOverflow
    }

    @Composable
    private fun WidthBox(width: Dp, content: @Composable () -> Unit) {
        // requiredSize simulates a window wider than the test device, anchored at the start edge.
        Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(width, 800.dp)) {
            AppTheme { content() }
        }
    }

    @Test
    fun topBar_searchAndFavoriteActions_areLabelledButtonsThatNavigate() = runComposeUiTest {
        val repository = FakeCharacterRepository().apply { refreshResult = listOf(rick) }
        var searches = 0
        var favorites = 0

        setContent {
            CharacterScreen(
                onSearchClick = { searches++ },
                onFavoritesClick = { favorites++ },
                viewModel = createViewModel(repository)
            )
        }

        // Title is a heading; the icon actions are announced by their labels as buttons.
        onNodeWithText("Karakter").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        onNodeWithContentDescription("Cari")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        onNodeWithContentDescription("Favorit").assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, searches)
        assertEquals(1, favorites)
    }

    @Test
    fun whenDatabaseEmptyAndInitialRefreshRunning_showsLoadingNotEmptyState() = runComposeUiTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeCharacterRepository().apply {
            this.gate = gate
            refreshResult = listOf(rick)
        }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
        onNodeWithText("Belum ada karakter").assertDoesNotExist()

        gate.complete(Unit)

        onNodeWithText("Rick Sanchez").assertIsDisplayed()
    }

    @Test
    fun whenCacheExistsAndRefreshRunning_keepsListAndShowsRefreshIndicator() = runComposeUiTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeCharacterRepository().apply {
            characters.value = listOf(rick)
            this.gate = gate
            refreshResult = listOf(rick, morty)
        }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }

        // Cache tetap tampil, dan hanya indikator pull-to-refresh yang berputar.
        onNodeWithText("Rick Sanchez").assertIsDisplayed()
        onNode(refreshingIndicator).assertExists()
        onAllNodes(anyIndeterminateProgress).assertCountEquals(1)

        gate.complete(Unit)

        // Data baru datang lewat Room Flow, indikator berhenti.
        onNodeWithText("Morty Smith").assertIsDisplayed()
        onNode(refreshingIndicator).assertDoesNotExist()
        onAllNodes(anyIndeterminateProgress).assertCountEquals(0)
    }

    @Test
    fun whenPullToRefreshFails_keepsCacheAndShowsWarning() = runComposeUiTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeCharacterRepository().apply {
            characters.value = listOf(rick)
            this.gate = gate
            errorToThrow = DataError.NoConnection()
        }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }

        onNodeWithText("Rick Sanchez").assertIsDisplayed()
        onNode(refreshingIndicator).assertExists()

        gate.complete(Unit)

        onNodeWithText("Gagal memperbarui data. Menampilkan data tersimpan.").assertIsDisplayed()
        onNodeWithText("Rick Sanchez").assertIsDisplayed()
        onNode(refreshingIndicator).assertDoesNotExist()
    }

    @Test
    fun swipeDownOnList_triggersRefresh() = runComposeUiTest {
        val repository = FakeCharacterRepository().apply {
            characters.value = listOf(rick)
            refreshResult = listOf(rick)
        }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }
        waitForIdle()
        assertEquals(1, repository.refreshCount)
        onNodeWithText("Morty Smith").assertDoesNotExist()

        repository.refreshResult = listOf(rick, morty)

        onNode(hasScrollAction()).performTouchInput { swipeDown() }

        onNodeWithText("Morty Smith").assertIsDisplayed()
        assertEquals(2, repository.refreshCount)
    }

    @Test
    fun whenRefreshFailsWithoutCache_showsErrorAndRetryWorks() = runComposeUiTest {
        val repository = FakeCharacterRepository().apply {
            errorToThrow = DataError.NoConnection()
        }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }

        onNodeWithText("Gagal memuat karakter. Periksa koneksi internet.").assertIsDisplayed()

        // Internet "pulih", user menekan tombol Coba Lagi
        repository.errorToThrow = null
        repository.refreshResult = listOf(rick)
        onNodeWithText("Coba Lagi").performClick()

        onNodeWithText("Rick Sanchez").assertIsDisplayed()
    }

    @Test
    fun whenRefreshFailsWithCache_showsCachedListAndWarning() = runComposeUiTest {
        val repository = FakeCharacterRepository().apply {
            characters.value = listOf(rick)
            errorToThrow = DataError.NoConnection()
        }

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }

        onNodeWithText("Gagal memperbarui data. Menampilkan data tersimpan.").assertIsDisplayed()
        onNodeWithText("Rick Sanchez").assertIsDisplayed()
    }

    @Test
    fun whenApiReturnsEmpty_showsEmptyStateAndLoadButtonWorks() = runComposeUiTest {
        val repository = FakeCharacterRepository()

        setContent { CharacterScreen(viewModel = createViewModel(repository)) }

        onNodeWithText("Belum ada karakter").assertIsDisplayed()

        repository.refreshResult = listOf(morty)
        onNodeWithText("Muat Karakter").performClick()

        onNodeWithText("Morty Smith").assertIsDisplayed()
    }
}
