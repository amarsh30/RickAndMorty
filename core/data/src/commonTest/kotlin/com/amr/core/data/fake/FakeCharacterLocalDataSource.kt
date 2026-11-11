package com.amr.core.data.fake

import com.amr.core.data.local.datasource.CharacterLocalDataSource
import com.amr.core.data.local.model.CharacterPagingMetadata
import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Mirrors the Room DAO semantics proven by CharacterDaoTest:
 * - replaceFirstPage replaces the whole list,
 * - appendPage upserts by id,
 * - the list is ordered by id,
 * - characters and metadata change together, and metadata starts as null.
 */
class FakeCharacterLocalDataSource : CharacterLocalDataSource {

    private val characters = MutableStateFlow<List<Character>>(emptyList())
    private val metadata = MutableStateFlow<CharacterPagingMetadata?>(null)

    /** Jika diisi, operasi tulis berikutnya gagal tanpa mengubah data (seperti rollback). */
    var writeError: Exception? = null

    val currentCharacters: List<Character> get() = characters.value
    val currentMetadata: CharacterPagingMetadata? get() = metadata.value

    /** Mengisi cache awal seolah sudah tersimpan sebelumnya. */
    fun seed(characters: List<Character>, metadata: CharacterPagingMetadata?) {
        this.characters.value = characters.sortedBy { it.id }
        this.metadata.value = metadata
    }

    override fun observeCharacters(): Flow<List<Character>> = characters

    override fun observePagingMetadata(): Flow<CharacterPagingMetadata?> = metadata

    override suspend fun getPagingMetadata(): CharacterPagingMetadata? = metadata.value

    override suspend fun replaceFirstPage(
        characters: List<Character>,
        metadata: CharacterPagingMetadata
    ) {
        writeError?.let { throw it }
        this.characters.value = characters.sortedBy { it.id }
        this.metadata.value = metadata
    }

    override suspend fun appendPage(
        characters: List<Character>,
        metadata: CharacterPagingMetadata
    ) {
        writeError?.let { throw it }
        val byId = this.characters.value.associateBy { it.id } + characters.associateBy { it.id }
        this.characters.value = byId.values.sortedBy { it.id }
        this.metadata.value = metadata
    }
}
