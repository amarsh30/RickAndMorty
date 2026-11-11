package com.amr.feature.character.fake

import com.amr.core.domain.model.Character
import com.amr.core.domain.model.PaginationState
import com.amr.core.domain.repository.CharacterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCharacterRepository : CharacterRepository {

    val characters = MutableStateFlow<List<Character>>(emptyList())

    var refreshResult: List<Character> = emptyList()
    var errorToThrow: Exception? = null

    /** Jika diisi, refresh akan menunggu sampai gate di-complete (untuk uji state isRefreshing). */
    var gate: CompletableDeferred<Unit>? = null

    var refreshCount = 0
        private set

    override fun observeCharacters(): Flow<List<Character>> = characters

    override suspend fun refreshCharacters() {
        refreshCount++
        gate?.await()
        errorToThrow?.let { throw it }
        characters.value = refreshResult
    }

    /** Tetap Uninitialized kecuali diubah test, sehingga test lama tidak memicu load-more. */
    val paginationState = MutableStateFlow<PaginationState>(PaginationState.Uninitialized)

    /** Karakter yang ditambahkan (seperti appendPage) saat loadNextPage berhasil. */
    var nextPageResult: List<Character> = emptyList()

    /** State pagination setelah loadNextPage berhasil. */
    var paginationStateAfterLoad: PaginationState = PaginationState.HasMore

    var loadNextPageError: Exception? = null

    /** Jika diisi, loadNextPage menunggu gate ini (untuk uji isLoadingMore). */
    var loadNextPageGate: CompletableDeferred<Unit>? = null

    var loadNextPageCount = 0
        private set

    /** Jika diisi, dipakai sebagai pengganti [paginationState] (mis. Flow yang gagal). */
    var paginationFlowOverride: Flow<PaginationState>? = null

    override fun observePaginationState(): Flow<PaginationState> =
        paginationFlowOverride ?: paginationState

    override suspend fun loadNextPage() {
        loadNextPageCount++
        loadNextPageGate?.await()
        loadNextPageError?.let { throw it }
        characters.value = (characters.value + nextPageResult).distinctBy { it.id }
        paginationState.value = paginationStateAfterLoad
    }
}
