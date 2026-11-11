package com.amr.core.data.fake

import com.amr.core.data.remote.datasource.EpisodeRemoteDataSource
import com.amr.core.data.remote.datasource.LocationRemoteDataSource
import com.amr.core.data.remote.dto.EpisodeDto
import com.amr.core.data.remote.dto.EpisodeResponseDto
import com.amr.core.data.remote.dto.LocationDto
import com.amr.core.data.remote.dto.LocationResponseDto
import com.amr.core.data.remote.dto.PageInfoDto
import com.amr.core.database.dao.EpisodeDao
import com.amr.core.database.dao.LocationDao
import com.amr.core.database.entity.EpisodeCharacterEntity
import com.amr.core.database.entity.EpisodeEntity
import com.amr.core.database.entity.EpisodePagingStateEntity
import com.amr.core.database.entity.LocationEntity
import com.amr.core.database.entity.LocationPagingStateEntity
import com.amr.core.database.entity.LocationResidentEntity
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

fun testLocationDto(id: Int, vararg residentIds: Int) = LocationDto(
    id = id,
    name = "Location $id",
    type = "Planet",
    dimension = "unknown",
    residents = residentIds.map { "https://rickandmortyapi.com/api/character/$it" }
)

fun testEpisodeDto(id: Int, vararg characterIds: Int) = EpisodeDto(
    id = id,
    name = "Episode $id",
    airDate = "December 2, 2013",
    code = "S01E0$id",
    characters = characterIds.map { "https://rickandmortyapi.com/api/character/$it" }
)

fun pageInfo(page: Int, pages: Int, count: Int = pages * 20) = PageInfoDto(
    count = count,
    pages = pages,
    next = if (page < pages) "https://rickandmortyapi.com/api/x?page=${page + 1}" else null,
    prev = null
)

class FakeLocationRemoteDataSource : LocationRemoteDataSource {
    val pages = mutableMapOf<Int, LocationResponseDto>()
    val details = mutableMapOf<Int, LocationDto>()
    var error: Exception? = null
    val requestedPages = mutableListOf<Int>()

    override suspend fun getLocations(page: Int): LocationResponseDto {
        requestedPages += page
        error?.let { throw it }
        return pages[page] ?: throw NetworkException.NotFound()
    }

    override suspend fun getLocation(id: Int): LocationDto {
        error?.let { throw it }
        return details[id] ?: throw NetworkException.NotFound()
    }
}

class FakeEpisodeRemoteDataSource : EpisodeRemoteDataSource {
    val pages = mutableMapOf<Int, EpisodeResponseDto>()
    val details = mutableMapOf<Int, EpisodeDto>()
    var error: Exception? = null

    override suspend fun getEpisodes(page: Int): EpisodeResponseDto {
        error?.let { throw it }
        return pages[page] ?: throw NetworkException.NotFound()
    }

    override suspend fun getEpisode(id: Int): EpisodeDto {
        error?.let { throw it }
        return details[id] ?: throw NetworkException.NotFound()
    }
}

/**
 * In-memory [LocationDao]. The transactional default methods (replaceFirstPage,
 * appendPage, saveDetail) are inherited, so their logic is exercised as written;
 * atomicity and foreign keys are verified on Room in :core:database.
 */
class FakeLocationDao : LocationDao {
    val locations = MutableStateFlow<Map<Int, LocationEntity>>(emptyMap())
    val residents = MutableStateFlow<List<LocationResidentEntity>>(emptyList())
    val pagingState = MutableStateFlow<LocationPagingStateEntity?>(null)

    override fun observeListLocations(): Flow<List<LocationEntity>> =
        locations.map { all -> all.values.filter { it.inList }.sortedBy { it.id } }

    override fun observeLocation(id: Int): Flow<LocationEntity?> = locations.map { it[id] }

    override fun observeResidentIds(locationId: Int): Flow<List<Int>> =
        combine(locations, residents) { _, rows ->
            rows.filter { it.locationId == locationId }.sortedBy { it.position }.map { it.characterId }
        }

    override suspend fun getPagingState() = pagingState.value
    override fun observePagingState(): Flow<LocationPagingStateEntity?> = pagingState
    override suspend fun upsertPagingState(state: LocationPagingStateEntity) {
        pagingState.value = state
    }

    override suspend fun upsertLocations(locations: List<LocationEntity>) {
        this.locations.value = this.locations.value + locations.associateBy { it.id }
    }

    override suspend fun clearListMembership() {
        locations.value = locations.value.mapValues { (_, row) -> row.copy(inList = false) }
    }

    override suspend fun updateLocationFields(id: Int, name: String, type: String, dimension: String): Int {
        val row = locations.value[id] ?: return 0
        locations.value = locations.value + (id to row.copy(name = name, type = type, dimension = dimension))
        return 1
    }

    override suspend fun insertLocationIfAbsent(location: LocationEntity) {
        if (location.id !in locations.value) locations.value = locations.value + (location.id to location)
    }

    override suspend fun deleteResidents(locationIds: List<Int>) {
        residents.value = residents.value.filterNot { it.locationId in locationIds }
    }

    override suspend fun insertResidents(residents: List<LocationResidentEntity>) {
        this.residents.value = this.residents.value + residents
    }
}

/** In-memory [EpisodeDao]; see [FakeLocationDao]. */
class FakeEpisodeDao : EpisodeDao {
    val episodes = MutableStateFlow<Map<Int, EpisodeEntity>>(emptyMap())
    val links = MutableStateFlow<List<EpisodeCharacterEntity>>(emptyList())
    val pagingState = MutableStateFlow<EpisodePagingStateEntity?>(null)

    override fun observeListEpisodes(): Flow<List<EpisodeEntity>> =
        episodes.map { all -> all.values.filter { it.inList }.sortedBy { it.id } }

    override fun observeEpisode(id: Int): Flow<EpisodeEntity?> = episodes.map { it[id] }

    override fun observeCharacterIds(episodeId: Int): Flow<List<Int>> =
        links.map { rows -> rows.filter { it.episodeId == episodeId }.sortedBy { it.position }.map { it.characterId } }

    override suspend fun getPagingState() = pagingState.value
    override fun observePagingState(): Flow<EpisodePagingStateEntity?> = pagingState
    override suspend fun upsertPagingState(state: EpisodePagingStateEntity) {
        pagingState.value = state
    }

    override suspend fun upsertEpisodes(episodes: List<EpisodeEntity>) {
        this.episodes.value = this.episodes.value + episodes.associateBy { it.id }
    }

    override suspend fun clearListMembership() {
        episodes.value = episodes.value.mapValues { (_, row) -> row.copy(inList = false) }
    }

    override suspend fun updateEpisodeFields(id: Int, name: String, airDate: String, code: String): Int {
        val row = episodes.value[id] ?: return 0
        episodes.value = episodes.value + (id to row.copy(name = name, airDate = airDate, code = code))
        return 1
    }

    override suspend fun insertEpisodeIfAbsent(episode: EpisodeEntity) {
        if (episode.id !in episodes.value) episodes.value = episodes.value + (episode.id to episode)
    }

    override suspend fun deleteCharacterLinks(episodeIds: List<Int>) {
        links.value = links.value.filterNot { it.episodeId in episodeIds }
    }

    override suspend fun insertCharacterLinks(links: List<EpisodeCharacterEntity>) {
        this.links.value = this.links.value + links
    }
}
