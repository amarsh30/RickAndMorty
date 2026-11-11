package com.amr.feature.character.presentation

import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.PaginationState
import com.amr.core.domain.repository.CharacterRepository
import com.amr.core.domain.usecase.LoadNextPageCharactersUseCase
import com.amr.core.domain.usecase.ObserveCharactersUseCase
import com.amr.core.domain.usecase.ObservePaginationStateUseCase
import com.amr.core.domain.usecase.RefreshCharactersUseCase
import com.amr.feature.character.fake.FakeCharacterRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
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
        refreshCharactersUseCase = RefreshCharactersUseCase(repository),
        observePaginationStateUseCase = ObservePaginationStateUseCase(repository),
        loadNextPageCharactersUseCase = LoadNextPageCharactersUseCase(repository)
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
    fun init_whenRefreshFailsBeforeCacheEmits_keepsCacheWarning() = runTest(testDispatcher) {
        repository.errorToThrow = DataError.NoConnection()

        val viewModel = createViewModel()
        advanceUntilIdle()
        // Refresh sudah gagal, Room belum mengirim cache apa pun.
        assertEquals(
            "Gagal memuat karakter. Periksa koneksi internet.",
            viewModel.uiState.value.errorMessage
        )

        // Room baru mengirim cache setelah kegagalan.
        repository.characters.value = listOf(rick)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(rick), state.characters)
        assertFalse(state.isLoading)
        assertEquals("Gagal memperbarui data. Menampilkan data tersimpan.", state.errorMessage)
    }

    @Test
    fun roomReEmitAfterRefreshFailure_keepsCacheWarning() = runTest(testDispatcher) {
        repository.characters.value = listOf(rick)
        repository.errorToThrow = DataError.NoConnection()

        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.characters.value = listOf(rick, morty)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(rick, morty), state.characters)
        assertEquals("Gagal memperbarui data. Menampilkan data tersimpan.", state.errorMessage)
    }

    @Test
    fun refreshSucceedsAfterCacheWarning_clearsWarning() = runTest(testDispatcher) {
        repository.characters.value = listOf(rick)
        repository.errorToThrow = DataError.NoConnection()
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.errorToThrow = null
        repository.refreshResult = listOf(rick, morty)
        viewModel.refreshCharacters()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(rick, morty), state.characters)
        assertNull(state.errorMessage)
    }

    // --- Pagination / load-more (E6) ---

    private val summer = Character(3, "Summer Smith", "Alive", "Human", "summer.png")

    @Test
    fun paginationHasMore_enablesLoadMore() = runTest(testDispatcher) {
        repository.paginationState.value = PaginationState.HasMore

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canLoadMore)
        assertFalse(viewModel.uiState.value.isEndReached)
    }

    @Test
    fun paginationEndReached_disablesLoadMoreAndMarksEnd() = runTest(testDispatcher) {
        repository.paginationState.value = PaginationState.EndReached

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canLoadMore)
        assertTrue(viewModel.uiState.value.isEndReached)
    }

    @Test
    fun loadMore_whenHasMore_appendsCharactersFromRoom() = runTest(testDispatcher) {
        repository.refreshResult = listOf(rick, morty)
        repository.paginationState.value = PaginationState.HasMore
        repository.nextPageResult = listOf(summer)
        repository.paginationStateAfterLoad = PaginationState.EndReached
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadMoreCharacters()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(rick, morty, summer), state.characters)
        assertFalse(state.isLoadingMore)
        assertNull(state.loadMoreErrorMessage)
        assertTrue(state.isEndReached)
        assertFalse(state.canLoadMore)
        assertEquals(1, repository.loadNextPageCount)
    }

    @Test
    fun loadMore_whileLoadingMore_isIgnored() = runTest(testDispatcher) {
        repository.refreshResult = listOf(rick)
        repository.paginationState.value = PaginationState.HasMore
        val gate = CompletableDeferred<Unit>()
        repository.loadNextPageGate = gate
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadMoreCharacters()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLoadingMore)

        viewModel.loadMoreCharacters()
        advanceUntilIdle()
        assertEquals(1, repository.loadNextPageCount)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    @Test
    fun loadMore_whenUninitializedOrEndReached_doesNothing() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadMoreCharacters()
        advanceUntilIdle()

        repository.paginationState.value = PaginationState.EndReached
        advanceUntilIdle()
        viewModel.loadMoreCharacters()
        advanceUntilIdle()

        assertEquals(0, repository.loadNextPageCount)
    }

    @Test
    fun loadMore_whileRefreshing_isIgnored() = runTest(testDispatcher) {
        repository.characters.value = listOf(rick)
        repository.paginationState.value = PaginationState.HasMore
        val refreshGate = CompletableDeferred<Unit>()
        repository.gate = refreshGate
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isRefreshing)

        viewModel.loadMoreCharacters()
        advanceUntilIdle()

        assertEquals(0, repository.loadNextPageCount)
        refreshGate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun loadMore_whenFails_showsFooterErrorAndKeepsList() = runTest(testDispatcher) {
        repository.refreshResult = listOf(rick, morty)
        repository.paginationState.value = PaginationState.HasMore
        repository.loadNextPageError = DataError.NoConnection()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadMoreCharacters()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(rick, morty), state.characters)
        assertEquals("Gagal memuat karakter. Periksa koneksi internet.", state.loadMoreErrorMessage)
        // The screen-level error is untouched by a load-more failure.
        assertNull(state.errorMessage)
        assertFalse(state.isLoadingMore)
        assertTrue(state.canLoadMore)
    }

    @Test
    fun loadMore_retryAfterFailure_clearsErrorAndAppends() = runTest(testDispatcher) {
        repository.refreshResult = listOf(rick)
        repository.paginationState.value = PaginationState.HasMore
        repository.loadNextPageError = DataError.Timeout()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadMoreCharacters()
        advanceUntilIdle()

        repository.loadNextPageError = null
        repository.nextPageResult = listOf(morty)
        viewModel.loadMoreCharacters()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(rick, morty), state.characters)
        assertNull(state.loadMoreErrorMessage)
        assertEquals(2, repository.loadNextPageCount)
    }

    @Test
    fun loadMore_whenCancelled_showsNoError() = runTest(testDispatcher) {
        repository.paginationState.value = PaginationState.HasMore
        repository.loadNextPageError = CancellationException("cancelled")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadMoreCharacters()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadMoreErrorMessage)
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    @Test
    fun refresh_clearsLoadMoreError() = runTest(testDispatcher) {
        repository.refreshResult = listOf(rick)
        repository.paginationState.value = PaginationState.HasMore
        repository.loadNextPageError = DataError.NoConnection()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadMoreCharacters()
        advanceUntilIdle()

        viewModel.refreshCharacters()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadMoreErrorMessage)
    }

    @Test
    fun paginationFlowFails_disablesLoadMoreWithoutCrashing() = runTest(testDispatcher) {
        repository.refreshResult = listOf(rick)
        repository.paginationFlowOverride = flow { throw DataError.InvalidData() }

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.canLoadMore)
        assertFalse(state.isEndReached)
        assertEquals(listOf(rick), state.characters)
    }

    // --- Room observation errors (E4-B) ---

    private fun createViewModel(repository: ControllableRepository) = CharacterViewModel(
        observeCharactersUseCase = ObserveCharactersUseCase(repository),
        refreshCharactersUseCase = RefreshCharactersUseCase(repository),
        observePaginationStateUseCase = ObservePaginationStateUseCase(repository),
        loadNextPageCharactersUseCase = LoadNextPageCharactersUseCase(repository)
    )

    @Test
    fun observeFailsBeforeFirstEmission_stopsLoadingAndShowsDatabaseError() = runTest(testDispatcher) {
        val repository = ControllableRepository().apply {
            observations.add(flow { throw IllegalStateException("DB open failed") })
        }

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.characters.isEmpty())
        assertEquals(DATABASE_ERROR, state.errorMessage)
    }

    @Test
    fun observeFailsAfterCache_keepsCharactersAndShowsDatabaseError() = runTest(testDispatcher) {
        val repository = ControllableRepository().apply {
            observations.add(flow {
                emit(listOf(rick))
                throw IllegalStateException("Mapper failed")
            })
        }

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(rick), state.characters)
        assertEquals(DATABASE_ERROR, state.errorMessage)
    }

    @Test
    fun retryAfterObserveFailure_restartsCollectorAndRecovers() = runTest(testDispatcher) {
        val room = MutableSharedFlow<List<Character>>()
        val repository = ControllableRepository().apply {
            observations.add(flow { throw IllegalStateException("DB open failed") })
            observations.add(room)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.refreshCharacters()
        advanceUntilIdle()

        assertEquals(2, repository.subscriptionCount)
        assertEquals(1, repository.activeCollectors)
        // Refresh sukses, tetapi Room belum pulih: error database tetap tampil.
        assertEquals(2, repository.refreshCount)
        assertEquals(DATABASE_ERROR, viewModel.uiState.value.errorMessage)

        room.emit(listOf(rick, morty))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(rick, morty), state.characters)
        assertNull(state.errorMessage)
    }

    @Test
    fun refreshFailsWhileObserveFailed_keepsDatabaseError() = runTest(testDispatcher) {
        val repository = ControllableRepository().apply {
            observations.add(flow {
                emit(listOf(rick))
                throw IllegalStateException("Mapper failed")
            })
            observations.add(MutableSharedFlow())
            refreshError = DataError.NoConnection()
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.refreshCharacters()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(rick), state.characters)
        assertEquals(DATABASE_ERROR, state.errorMessage)
    }

    @Test
    fun roomRecoversThenRefreshFails_showsCacheWithRefreshWarning() = runTest(testDispatcher) {
        val room = MutableSharedFlow<List<Character>>()
        val refreshGate = CompletableDeferred<Unit>()
        val repository = ControllableRepository().apply {
            observations.add(flow { throw IllegalStateException("DB open failed") })
            observations.add(room)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        assertEquals(DATABASE_ERROR, viewModel.uiState.value.errorMessage)

        // Retry: observasi dimulai ulang, refresh API ditahan di gate.
        repository.refreshGate = refreshGate
        repository.refreshError = DataError.NoConnection()
        viewModel.refreshCharacters()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isRefreshing)

        // Room pulih lebih dulu dan mengirim cache.
        room.emit(listOf(rick))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)

        // Baru kemudian refresh API gagal karena tidak ada koneksi.
        refreshGate.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(rick), state.characters)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertEquals("Gagal memperbarui data. Menampilkan data tersimpan.", state.errorMessage)
    }

    @Test
    fun refreshFailsBeforeRoomRecovers_showsCacheWithRefreshWarning() = runTest(testDispatcher) {
        val room = MutableSharedFlow<List<Character>>()
        val repository = ControllableRepository().apply {
            observations.add(flow { throw IllegalStateException("DB open failed") })
            observations.add(room)
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        assertEquals(DATABASE_ERROR, viewModel.uiState.value.errorMessage)

        // Retry: observasi dimulai ulang, tetapi refresh gagal sebelum Room emit.
        repository.refreshError = DataError.NoConnection()
        viewModel.refreshCharacters()
        advanceUntilIdle()
        assertEquals(DATABASE_ERROR, viewModel.uiState.value.errorMessage)

        // Room baru pulih setelah refresh gagal.
        room.emit(listOf(rick))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(rick), state.characters)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertEquals("Gagal memperbarui data. Menampilkan data tersimpan.", state.errorMessage)
    }

    @Test
    fun observeThrowsCancellation_isNotTurnedIntoError() = runTest(testDispatcher) {
        val repository = ControllableRepository().apply {
            observations.add(flow { throw CancellationException("cancelled") })
        }

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertTrue(state.isLoading)
    }

    @Test
    fun repeatedRefresh_doesNotCreateDuplicateCollectors() = runTest(testDispatcher) {
        val repository = ControllableRepository().apply {
            observations.add(MutableSharedFlow())
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        repeat(3) {
            viewModel.refreshCharacters()
            advanceUntilIdle()
        }

        assertEquals(1, repository.subscriptionCount)
        assertEquals(1, repository.activeCollectors)
        assertEquals(4, repository.refreshCount)
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

    private companion object {
        const val DATABASE_ERROR = "Gagal membaca data tersimpan. Coba lagi."
    }

    /**
     * Repository dengan Flow observasi yang dikontrol per subscription, untuk
     * mensimulasikan kegagalan Room/mapper dan menghitung collector aktif.
     * Refresh tidak menulis data: data hanya datang dari Flow observasi.
     */
    private class ControllableRepository : CharacterRepository {

        /** Flow untuk tiap subscription berikutnya; jika habis, Flow tidak pernah emit. */
        val observations = ArrayDeque<Flow<List<Character>>>()
        var refreshError: Exception? = null

        /** Jika diisi, refresh menunggu gate sebelum berhasil/gagal. */
        var refreshGate: CompletableDeferred<Unit>? = null

        var subscriptionCount = 0
            private set
        var activeCollectors = 0
            private set
        var refreshCount = 0
            private set

        override fun observeCharacters(): Flow<List<Character>> {
            val source = observations.removeFirstOrNull() ?: MutableSharedFlow()
            return source
                .onStart {
                    subscriptionCount++
                    activeCollectors++
                }
                .onCompletion { activeCollectors-- }
        }

        override suspend fun refreshCharacters() {
            refreshCount++
            refreshGate?.await()
            refreshError?.let { throw it }
        }

        // Pagination is not used by these tests.
        override fun observePaginationState(): Flow<PaginationState> =
            flowOf(PaginationState.Uninitialized)

        override suspend fun loadNextPage() = Unit
    }
}
