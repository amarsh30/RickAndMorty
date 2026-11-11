package com.amr.core.data.remote.datasource

import com.amr.core.data.remote.dto.CharacterResponseDto

interface CharacterRemoteDataSource {
    suspend fun getCharacters(page: Int = FIRST_PAGE): CharacterResponseDto

    companion object {
        const val FIRST_PAGE = 1
    }
}
