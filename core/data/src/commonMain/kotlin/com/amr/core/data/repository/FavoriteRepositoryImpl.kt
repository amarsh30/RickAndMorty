package com.amr.core.data.repository

import com.amr.core.data.local.datasource.CharacterLookupLocalDataSource
import com.amr.core.domain.model.Character
import com.amr.core.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

class FavoriteRepositoryImpl(
    private val localDataSource: CharacterLookupLocalDataSource
) : FavoriteRepository {

    override fun observeFavorites(): Flow<List<Character>> = localDataSource.observeFavorites()

    override fun observeIsFavorite(id: Int): Flow<Boolean> = localDataSource.observeIsFavorite(id)

    override suspend fun addFavorite(character: Character) {
        localDataSource.addFavorite(character)
    }

    override suspend fun removeFavorite(id: Int) {
        localDataSource.removeFavorite(id)
    }
}
