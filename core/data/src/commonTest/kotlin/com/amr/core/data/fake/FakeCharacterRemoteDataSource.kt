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
            testCharacterDto(1, "Rick Sanchez", imageUrl = "https://example.com/rick.png"),
            testCharacterDto(2, "Morty Smith", imageUrl = "https://example.com/morty.png")
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

    // --- search ---

    data class SearchQuery(val name: String?, val status: String?, val species: String?, val gender: String?)

    val searchQueries = mutableListOf<SearchQuery>()
    var searchResponse: CharacterResponseDto = response
    var searchError: Exception? = null

    override suspend fun searchCharacters(
        name: String?,
        status: String?,
        species: String?,
        gender: String?
    ): CharacterResponseDto {
        searchQueries += SearchQuery(name, status, species, gender)
        searchError?.let { throw it }
        return searchResponse
    }

    // --- detail ---

    val requestedCharacterIds = mutableListOf<Int>()
    val characters = mutableMapOf<Int, CharacterDto>()
    var characterError: Exception? = null

    override suspend fun getCharacter(id: Int): CharacterDto {
        requestedCharacterIds += id
        characterError?.let { throw it }
        return characters[id] ?: throw NetworkException.NotFound()
    }

    fun failWithNoInternet() {
        errorToThrow = NetworkException.NoInternet()
    }
}
