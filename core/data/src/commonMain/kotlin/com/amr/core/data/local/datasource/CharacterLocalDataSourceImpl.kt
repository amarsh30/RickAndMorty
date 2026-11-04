package com.amr.core.data.local.datasource

import com.amr.core.data.local.datasource.CharacterLocalDataSource
import com.amr.core.database.dao.CharacterDao
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CharacterLocalDataSourceImpl(
    private val characterDao: CharacterDao
) : CharacterLocalDataSource {

    override fun observeCharacters(): Flow<List<Character>> {
        return characterDao.observeCharacters()
            .map { entities ->
                entities.map { entity ->
                    Character(
                        id = entity.id,
                        name = entity.name,
                        status = entity.status,
                        species = entity.species,
                        imageUrl = entity.imageUrl
                    )
                }
            }
    }

    override suspend fun saveCharacters(
        characters: List<Character>
    ) {
        val entities = characters.map { character ->
            CharacterEntity(
                id = character.id,
                name = character.name,
                status = character.status,
                species = character.species,
                imageUrl = character.imageUrl
            )
        }

        characterDao.insertCharacters(entities)
    }
}