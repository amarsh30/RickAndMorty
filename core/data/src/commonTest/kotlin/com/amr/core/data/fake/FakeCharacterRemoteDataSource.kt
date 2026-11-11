package com.amr.core.data.fake

import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.data.remote.dto.CharacterDto
import com.amr.core.data.remote.dto.CharacterResponseDto
import com.amr.core.data.remote.dto.PageInfoDto
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.CompletableDeferred

class FakeCharacterRemoteDataSource : CharacterRemoteDataSource {

    /** Response bawaan, dipakai untuk halaman yang tidak ada di [responses]. */
    var response = CharacterResponseDto(
        info = PageInfoDto(count = 2, pages = 1, next = null, prev = null),
        results = listOf(
            CharacterDto(
                id = 1,
                name = "Rick Sanchez",
                status = "Alive",
                species = "Human",
                imageUrl = "https://example.com/rick.png"
            ),
            CharacterDto(
                id = 2,
                name = "Morty Smith",
                status = "Alive",
                species = "Human",
                imageUrl = "https://example.com/morty.png"
            )
        )
    )

    /** Response per halaman. */
    val responses = mutableMapOf<Int, CharacterResponseDto>()

    /** Jika diisi, getCharacters() akan melempar exception ini. */
    var errorToThrow: Exception? = null

    /** Jika diisi, setiap request menunggu gate ini sebelum menjawab. */
    var gate: CompletableDeferred<Unit>? = null

    /** Urutan halaman yang diminta. */
    val requestedPages = mutableListOf<Int>()

    val callCount: Int get() = requestedPages.size

    override suspend fun getCharacters(page: Int): CharacterResponseDto {
        requestedPages += page
        gate?.await()
        errorToThrow?.let { throw it }
        return responses[page] ?: response
    }

    fun failWithNoInternet() {
        errorToThrow = NetworkException.NoInternet()
    }
}
