package com.amr.core.domain.repository

import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.Flow

/** Favorites are stored locally only and survive list refreshes. */
interface FavoriteRepository {

    fun observeFavorites(): Flow<List<Character>>

    fun observeIsFavorite(id: Int): Flow<Boolean>

    suspend fun addFavorite(character: Character)

    suspend fun removeFavorite(id: Int)
}
