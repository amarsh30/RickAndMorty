package com.amr.core.data.local.datasource

import com.amr.core.data.mapper.toDomain
import com.amr.core.data.mapper.toEntity
import com.amr.core.database.dao.CharacterDao
import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CharacterLocalDataSourceImpl(
    private val characterDao: CharacterDao
) : CharacterLocalDataSource {

    override fun observeCharacters(): Flow<List<Character>> {
        return characterDao.observeCharacters()
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override suspend fun saveCharacters(
        characters: List<Character>
    ) {
        characterDao.insertCharacters(
            characters.map { it.toEntity() }
        )
    }
}