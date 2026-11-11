package com.amr.core.data.repository

import com.amr.core.data.fake.FakeEpisodeDao
import com.amr.core.data.fake.FakeEpisodeRemoteDataSource
import com.amr.core.data.fake.FakeLocationDao
import com.amr.core.data.fake.FakeLocationRemoteDataSource
import com.amr.core.data.fake.pageInfo
import com.amr.core.data.fake.testEpisodeDto
import com.amr.core.data.fake.testLocationDto
import com.amr.core.data.remote.dto.EpisodeResponseDto
import com.amr.core.data.remote.dto.LocationResponseDto
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Location
import com.amr.core.domain.model.PaginationState
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class LocationEpisodeRepositoriesTest {

    private lateinit var locationRemote: FakeLocationRemoteDataSource
    private lateinit var locationDao: FakeLocationDao
    private lateinit var locations: LocationRepositoryImpl
    private lateinit var episodeRemote: FakeEpisodeRemoteDataSource
    private lateinit var episodeDao: FakeEpisodeDao
    private lateinit var episodes: EpisodeRepositoryImpl

    @BeforeTest
    fun setUp() {
        locationRemote = FakeLocationRemoteDataSource()
        locationDao = FakeLocationDao()
        locations = LocationRepositoryImpl(locationRemote, locationDao)
        episodeRemote = FakeEpisodeRemoteDataSource()
        episodeDao = FakeEpisodeDao()
        episodes = EpisodeRepositoryImpl(episodeRemote, episodeDao)

        locationRemote.pages[1] = LocationResponseDto(pageInfo(1, 2), listOf(testLocationDto(1, 3, 4), testLocationDto(2)))
        locationRemote.pages[2] = LocationResponseDto(pageInfo(2, 2), listOf(testLocationDto(21, 5)))
    }

    private fun location(id: Int) = Location(id, "Location $id", "Planet", "unknown")

    // --- location list ---

    @Test
    fun beforeAnyLoad_listIsEmptyAndUninitialized() = runTest {
        assertEquals(emptyList(), locations.observeLocations().first())
        assertEquals(PaginationState.Uninitialized, locations.observePaginationState().first())
    }

    @Test
    fun refresh_storesFirstPageResidentsAndMetadata() = runTest {
        locations.refreshLocations()

        assertEquals(listOf(location(1), location(2)), locations.observeLocations().first())
        assertEquals(listOf(3, 4), locations.observeResidentIds(1).first())
        assertEquals(emptyList(), locations.observeResidentIds(2).first())
        assertEquals(PaginationState.HasMore, locations.observePaginationState().first())
    }

    @Test
    fun loadNextPage_appendsUntilEnd_thenDoesNothing() = runTest {
        locations.refreshLocations()

        locations.loadNextPage()
        locations.loadNextPage()

        assertEquals(listOf(1, 2, 21), locations.observeLocations().first().map { it.id })
        assertEquals(PaginationState.EndReached, locations.observePaginationState().first())
        assertEquals(listOf(1, 2), locationRemote.requestedPages)
    }

    @Test
    fun loadNextPage_withoutMetadata_loadsFirstPage() = runTest {
        locations.loadNextPage()

        assertEquals(listOf(1), locationRemote.requestedPages)
        assertEquals(2, locations.observeLocations().first().size)
    }

    @Test
    fun refresh_offline_throwsNoConnectionAndKeepsCache() = runTest {
        locations.refreshLocations()
        locationRemote.error = NetworkException.NoInternet()

        assertFailsWith<DataError.NoConnection> { locations.refreshLocations() }

        assertEquals(2, locations.observeLocations().first().size)
        assertEquals(PaginationState.HasMore, locations.observePaginationState().first())
    }

    @Test
    fun refresh_inconsistentPageInfo_isInvalidDataAndKeepsCache() = runTest {
        locations.refreshLocations()
        // Claims a next page on the last page.
        locationRemote.pages[1] = LocationResponseDto(
            pageInfo(1, 1).copy(next = "https://rickandmortyapi.com/api/location?page=2"),
            listOf(testLocationDto(99))
        )

        assertFailsWith<DataError.InvalidData> { locations.refreshLocations() }

        assertEquals(listOf(1, 2), locations.observeLocations().first().map { it.id })
    }

    @Test
    fun refresh_keepsDetailOnlyLocationsOutOfTheList() = runTest {
        locationRemote.details[50] = testLocationDto(50, 8)
        locations.refreshLocation(50)

        locations.refreshLocations()

        assertEquals(listOf(1, 2), locations.observeLocations().first().map { it.id })
        assertEquals(location(50), locations.observeLocation(50).first())
        assertEquals(listOf(8), locations.observeResidentIds(50).first())
    }

    // --- location detail ---

    @Test
    fun refreshLocation_ofListedLocation_keepsItListed() = runTest {
        locations.refreshLocations()
        locationRemote.details[1] = testLocationDto(1, 3, 4, 9).copy(name = "Earth (C-137)")

        locations.refreshLocation(1)

        assertEquals("Earth (C-137)", locations.observeLocations().first().first().name)
        assertEquals(listOf(3, 4, 9), locations.observeResidentIds(1).first())
    }

    @Test
    fun refreshLocation_errors() = runTest {
        assertFailsWith<DataError.NotFound> { locations.refreshLocation(404) }

        locationRemote.details[2] = testLocationDto(3)
        assertFailsWith<DataError.InvalidData> { locations.refreshLocation(2) }

        locationRemote.error = NetworkException.NoInternet()
        assertFailsWith<DataError.NoConnection> { locations.refreshLocation(1) }
        assertNull(locations.observeLocation(1).first())
    }

    @Test
    fun duplicateResidentUrls_areStoredOnce() = runTest {
        locationRemote.details[7] = testLocationDto(7, 3, 3, 4)

        locations.refreshLocation(7)

        assertEquals(listOf(3, 4), locations.observeResidentIds(7).first())
    }

    // --- episodes ---

    @Test
    fun episodes_paginateAndKeepCharacterOrder() = runTest {
        episodeRemote.pages[1] = EpisodeResponseDto(pageInfo(1, 2), listOf(testEpisodeDto(1, 2, 1)))
        episodeRemote.pages[2] = EpisodeResponseDto(pageInfo(2, 2), listOf(testEpisodeDto(21, 5)))

        episodes.refreshEpisodes()
        episodes.loadNextPage()

        val listed = episodes.observeEpisodes().first()
        assertEquals(listOf(1, 21), listed.map { it.id })
        assertEquals("S01E01", listed.first().code)
        assertEquals("December 2, 2013", listed.first().airDate)
        assertEquals(listOf(2, 1), episodes.observeCharacterIds(1).first())
        assertEquals(PaginationState.EndReached, episodes.observePaginationState().first())
    }

    @Test
    fun episodeDetail_outsideList_isCachedButNotListed() = runTest {
        episodeRemote.details[9] = testEpisodeDto(9, 1)

        episodes.refreshEpisode(9)

        assertEquals(emptyList(), episodes.observeEpisodes().first())
        assertEquals(9, episodes.observeEpisode(9).first()?.id)
        assertFalse(episodeDao.episodes.value.getValue(9).inList)
    }
}
