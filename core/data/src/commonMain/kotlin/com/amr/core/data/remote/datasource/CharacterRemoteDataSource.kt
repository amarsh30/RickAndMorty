package com.amr.core.data.remote.datasource

import com.amr.core.data.remote.dto.CharacterDto
import com.amr.core.data.remote.dto.CharacterResponseDto

interface CharacterRemoteDataSource {
    suspend fun getCharacters(page: Int = FIRST_PAGE): CharacterResponseDto

    /** First page of characters matching the given API filters; null values are not sent. */
    suspend fun searchCharacters(
        name: String?,
        status: String?,
        species: String?,
        gender: String?
    ): CharacterResponseDto

    suspend fun getCharacter(id: Int): CharacterDto

    /**
     * The characters with the given ids (at most [MAX_IDS_PER_REQUEST]). Ids the API
     * does not know are missing from the result, as the API itself drops them.
     */
    suspend fun getCharactersByIds(ids: List<Int>): List<CharacterDto>

    companion object {
        const val FIRST_PAGE = 1
        const val MAX_IDS_PER_REQUEST = 50
    }
}
