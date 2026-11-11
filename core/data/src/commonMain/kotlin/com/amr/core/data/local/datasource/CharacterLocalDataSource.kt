package com.amr.core.data.local.datasource

import com.amr.core.data.local.model.CharacterPagingMetadata
import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.Flow

interface CharacterLocalDataSource {

    fun observeCharacters(): Flow<List<Character>>

    /** Emits null while no page has been stored yet. */
    fun observePagingMetadata(): Flow<CharacterPagingMetadata?>

    /** Returns null when no page has been stored yet. */
    suspend fun getPagingMetadata(): CharacterPagingMetadata?

    /** Atomically replaces all cached characters with page 1 and its metadata. */
    suspend fun replaceFirstPage(characters: List<Character>, metadata: CharacterPagingMetadata)

    /** Atomically upserts a following page and advances the metadata. */
    suspend fun appendPage(characters: List<Character>, metadata: CharacterPagingMetadata)
}
