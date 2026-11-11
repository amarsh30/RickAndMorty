package com.amr.feature.character.presentation

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
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
        loadNextPageCharactersUseCase = LoadNextPageCharactersUseCase(repository)
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

        onNodeWithText("Rick and Morty Characters").assertIsDisplayed()
        onNodeWithText("Rick Sanchez").assertIsDisplayed()
        onNodeWithText("Morty Smith").assertIsDisplayed()
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
