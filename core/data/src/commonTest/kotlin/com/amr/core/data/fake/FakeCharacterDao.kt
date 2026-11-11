package com.amr.core.data.fake

import com.amr.core.database.dao.CharacterDao
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.database.entity.CharacterPagingStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [CharacterDao] that stores rows exactly as given, including
 * invalid metadata rows (e.g. totalPages = null), so the real
 * CharacterLocalDataSourceImpl mapping can be tested against them.
 * replaceFirstPage/appendPage use the DAO's own default implementations.
 */
class FakeCharacterDao : CharacterDao {

    val characters = MutableStateFlow<List<CharacterEntity>>(emptyList())
    val pagingState = MutableStateFlow<CharacterPagingStateEntity?>(null)

    override fun observeCharacters(): Flow<List<CharacterEntity>> = characters

    override suspend fun insertCharacters(characters: List<CharacterEntity>) {
        val byId = this.characters.value.associateBy { it.id } + characters.associateBy { it.id }
        this.characters.value = byId.values.sortedBy { it.id }
    }

    override suspend fun deleteAllCharacters() {
        characters.value = emptyList()
    }

    override suspend fun getPagingState(): CharacterPagingStateEntity? = pagingState.value

    override fun observePagingState(): Flow<CharacterPagingStateEntity?> = pagingState

    override suspend fun upsertPagingState(state: CharacterPagingStateEntity) {
        pagingState.value = state
    }
}
