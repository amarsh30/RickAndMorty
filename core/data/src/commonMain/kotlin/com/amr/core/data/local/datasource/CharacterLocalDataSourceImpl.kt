package com.amr.core.data.local.datasource

import com.amr.core.data.local.model.CharacterPagingMetadata
import com.amr.core.data.mapper.toDomain
import com.amr.core.data.mapper.toEntity
import com.amr.core.database.dao.CharacterDao
import com.amr.core.database.entity.CharacterPagingStateEntity
import com.amr.core.domain.error.DataError
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

    override fun observePagingMetadata(): Flow<CharacterPagingMetadata?> {
        return characterDao.observePagingState()
            .map { it?.toMetadata() }
    }

    override suspend fun getPagingMetadata(): CharacterPagingMetadata? {
        return characterDao.getPagingState()?.toMetadata()
    }

    override suspend fun replaceFirstPage(
        characters: List<Character>,
        metadata: CharacterPagingMetadata
    ) {
        characterDao.replaceFirstPage(
            characters = characters.map { it.toEntity() },
            nextPage = metadata.nextPage,
            totalPages = metadata.totalPages
        )
    }

    override suspend fun appendPage(
        characters: List<Character>,
        metadata: CharacterPagingMetadata
    ) {
        characterDao.appendPage(
            characters = characters.map { it.toEntity() },
            nextPage = metadata.nextPage,
            totalPages = metadata.totalPages
        )
    }

    /**
     * Rows are only written with a known total. A row without one is corrupt:
     * it is reported as invalid data instead of being mistaken for "no page stored",
     * which would make load-more restart from page 1 and replace the cache.
     */
    private fun CharacterPagingStateEntity.toMetadata(): CharacterPagingMetadata {
        val total = totalPages ?: throw DataError.InvalidData()
        return CharacterPagingMetadata(nextPage = nextPage, totalPages = total)
    }
}
