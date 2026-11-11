package com.amr.feature.character.presentation

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.usecase.ObserveCharactersUseCase
import com.amr.core.domain.usecase.RefreshCharactersUseCase
import com.amr.feature.character.fake.FakeCharacterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test

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
        refreshCharactersUseCase = RefreshCharactersUseCase(repository)
    )

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

        onNodeWithText("Rick Sanchez").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()

        gate.complete(Unit)

        onNodeWithText("Morty Smith").assertIsDisplayed()
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
