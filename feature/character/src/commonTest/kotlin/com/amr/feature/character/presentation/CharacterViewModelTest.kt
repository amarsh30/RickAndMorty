package com.amr.feature.character.presentation

import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.usecase.ObserveCharactersUseCase
import com.amr.core.domain.usecase.RefreshCharactersUseCase
import com.amr.feature.character.fake.FakeCharacterRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
        repository.errorToThrow = DataError.NoConnection()

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
        repository.errorToThrow = DataError.NoConnection()

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
        repository.errorToThrow = DataError.NoConnection()
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

    private fun TestScope.errorMessageAfterInitWith(error: Exception): String? {
        repository.errorToThrow = error

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.characters.isEmpty())
        assertFalse(state.isRefreshing)
        return state.errorMessage
    }

    @Test
    fun init_whenTimeoutAndNoCache_showsTimeoutMessage() = runTest(testDispatcher) {
        assertEquals(
            "Server terlalu lama merespons. Coba lagi.",
            errorMessageAfterInitWith(DataError.Timeout())
        )
    }

    @Test
    fun init_whenServerErrorAndNoCache_showsServerMessage() = runTest(testDispatcher) {
        assertEquals(
            "Server sedang bermasalah. Coba lagi nanti.",
            errorMessageAfterInitWith(DataError.Server())
        )
    }

    @Test
    fun init_whenInvalidDataAndNoCache_showsInvalidDataMessage() = runTest(testDispatcher) {
        assertEquals(
            "Data dari server tidak dapat dibaca.",
            errorMessageAfterInitWith(DataError.InvalidData())
        )
    }

    @Test
    fun init_whenUnauthorizedAndNoCache_showsAccessDeniedMessage() = runTest(testDispatcher) {
        assertEquals(
            "Akses ke data ditolak.",
            errorMessageAfterInitWith(DataError.Unauthorized())
        )
    }

    @Test
    fun init_whenNotFoundAndNoCache_showsNotFoundMessage() = runTest(testDispatcher) {
        assertEquals(
            "Data karakter tidak ditemukan.",
            errorMessageAfterInitWith(DataError.NotFound())
        )
    }

    @Test
    fun init_whenUnknownAndNoCache_showsGenericMessage() = runTest(testDispatcher) {
        assertEquals(
            "Terjadi kesalahan. Coba lagi.",
            errorMessageAfterInitWith(DataError.Unknown())
        )
    }

    @Test
    fun init_whenNonDataErrorAndNoCache_showsGenericFallbackMessage() = runTest(testDispatcher) {
        assertEquals(
            "Terjadi kesalahan. Coba lagi.",
            errorMessageAfterInitWith(IllegalStateException("Database failure"))
        )
    }

    @Test
    fun init_whenDataErrorWithCache_keepsCharactersAndShowsCacheWarning() = runTest(testDispatcher) {
        repository.characters.value = listOf(rick, morty)
        repository.errorToThrow = DataError.Server()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(rick, morty), state.characters)
        assertFalse(state.isRefreshing)
        assertEquals("Gagal memperbarui data. Menampilkan data tersimpan.", state.errorMessage)
    }

    @Test
    fun init_whenRefreshCancelled_showsNoErrorAndResetsRefreshing() = runTest(testDispatcher) {
        repository.errorToThrow = CancellationException("cancelled")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertFalse(state.isRefreshing)
        assertEquals(1, repository.refreshCount)
    }
}
