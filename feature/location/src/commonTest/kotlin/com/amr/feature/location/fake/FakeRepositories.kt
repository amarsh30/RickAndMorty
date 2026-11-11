package com.amr.feature.location.fake

import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.Location
import com.amr.core.domain.model.PaginationState
import com.amr.core.domain.repository.LocationRepository
import com.amr.core.domain.repository.RelatedCharacterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeLocationRepository : LocationRepository {
    val list = MutableStateFlow<List<Location>>(emptyList())
    val pagination = MutableStateFlow<PaginationState>(PaginationState.Uninitialized)
    val details = MutableStateFlow<Map<Int, Location>>(emptyMap())
    val residentIds = MutableStateFlow<Map<Int, List<Int>>>(emptyMap())

    var refreshResult: List<Location>? = null
    var refreshError: Exception? = null
    var refreshGate: CompletableDeferred<Unit>? = null
    var nextPage: List<Location> = emptyList()
    var loadMoreError: Exception? = null
    var detailError: Exception? = null
    var refreshCalls = 0
    var loadMoreCalls = 0

    override fun observeLocations(): Flow<List<Location>> = list
    override fun observePaginationState(): Flow<PaginationState> = pagination

    override suspend fun refreshLocations() {
        refreshCalls++
        refreshGate?.await()
        refreshError?.let { throw it }
        refreshResult?.let { list.value = it }
        pagination.value = PaginationState.HasMore
    }

    override suspend fun loadNextPage() {
        loadMoreCalls++
        loadMoreError?.let { throw it }
        list.value = list.value + nextPage
        pagination.value = PaginationState.EndReached
    }

    override fun observeLocation(id: Int): Flow<Location?> = details.map { it[id] }
    override fun observeResidentIds(id: Int): Flow<List<Int>> = residentIds.map { it[id].orEmpty() }

    override suspend fun refreshLocation(id: Int) {
        detailError?.let { throw it }
    }
}

class FakeRelatedCharacterRepository : RelatedCharacterRepository {
    val cached = MutableStateFlow<Map<Int, Character>>(emptyMap())
    val fetchCalls = mutableListOf<List<Int>>()
    var fetchError: DataError? = null

    override fun observeCharacters(ids: List<Int>): Flow<List<Character>> =
        cached.map { all -> ids.mapNotNull { all[it] } }

    override suspend fun fetchMissing(ids: List<Int>) {
        fetchCalls += ids
        fetchError?.let { throw it }
        cached.value = cached.value + ids.associateWith { testCharacter(it) }
    }
}

fun testCharacter(id: Int) = Character(id, "Character $id", "Alive", "Human", null)
