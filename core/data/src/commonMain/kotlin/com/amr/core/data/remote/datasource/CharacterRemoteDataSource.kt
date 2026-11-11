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

    companion object {
        const val FIRST_PAGE = 1
    }
}
