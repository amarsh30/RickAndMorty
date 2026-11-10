package com.amr.feature.character.presentation

import com.amr.core.domain.model.Character
import com.amr.core.domain.usecase.ObserveCharactersUseCase
import com.amr.core.domain.usecase.RefreshCharactersUseCase
import com.amr.feature.character.fake.FakeCharacterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ViewModel memakai viewModelScope (Dispatchers.Main), jadi Main diganti
 * dengan TestDispatcher agar coroutine bisa dikontrol secara deterministik.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CharacterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCharacterRepository

    private val rick = Character(1, "Rick Sanchez", "Alive", "Human", "rick.png")
    private val morty = Character(2, "Morty Smith", "Alive", "Human", "morty.png")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeCharacterRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CharacterViewModel(
        observeCharactersUseCase = ObserveCharactersUseCase(repository),
        refreshCharactersUseCase = RefreshCharactersUseCase(repository)
    )

    @Test
    fun initialState_isLoading() {
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun init_whenRefreshSucceeds_showsCharacters() = runTest(testDispatcher) {
        repository.refreshResult = listOf(rick, morty)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertEquals(listOf(rick, morty), state.characters)
        assertNull(state.errorMessage)
        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun init_whenRefreshFailsAndNoCache_showsLoadError() = runTest(testDispatcher) {
        repository.errorToThrow = Exception("No internet")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.characters.isEmpty())
        assertFalse(state.isRefreshing)
        assertEquals("Gagal memuat karakter. Periksa koneksi internet.", state.errorMessage)
    }

    @Test
    fun init_whenRefreshFailsWithCache_showsCachedDataAndWarning() = runTest(testDispatcher) {
        repository.characters.value = listOf(rick)
        repository.errorToThrow = Exception("No internet")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(rick), state.characters)
        assertEquals("Gagal memperbarui data. Menampilkan data tersimpan.", state.errorMessage)
    }

    @Test
    fun refresh_whileRefreshing_isIgnored() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isRefreshing)

        viewModel.refreshCharacters()
        advanceUntilIdle()
        assertEquals(1, repository.refreshCount)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun refresh_afterError_clearsErrorOnSuccess() = runTest(testDispatcher) {
        repository.errorToThrow = Exception("No internet")
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.errorToThrow = null
        repository.refreshResult = listOf(morty)
        viewModel.refreshCharacters()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(morty), state.characters)
        assertNull(state.errorMessage)
        assertEquals(2, repository.refreshCount)
    }
}
