package com.amr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.amr.core.database.entity.LocationEntity
import com.amr.core.database.entity.LocationPagingStateEntity
import com.amr.core.database.entity.LocationResidentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Locations are written with upsert/update only: REPLACE would delete the row
 * first and cascade-delete its residents.
 */
@Dao
interface LocationDao {

    @Query("SELECT * FROM locations WHERE inList = 1 ORDER BY id ASC")
    fun observeListLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :id")
    fun observeLocation(id: Int): Flow<LocationEntity?>

    @Query("SELECT characterId FROM location_residents WHERE locationId = :locationId ORDER BY position ASC")
    fun observeResidentIds(locationId: Int): Flow<List<Int>>

    @Query("SELECT * FROM location_paging_state WHERE id = ${LocationPagingStateEntity.SINGLE_ROW_ID}")
    suspend fun getPagingState(): LocationPagingStateEntity?

    @Query("SELECT * FROM location_paging_state WHERE id = ${LocationPagingStateEntity.SINGLE_ROW_ID}")
    fun observePagingState(): Flow<LocationPagingStateEntity?>

    @Upsert
    suspend fun upsertPagingState(state: LocationPagingStateEntity)

    @Upsert
    suspend fun upsertLocations(locations: List<LocationEntity>)

    /** Removes every row from the list without deleting it. */
    @Query("UPDATE locations SET inList = 0 WHERE inList = 1")
    suspend fun clearListMembership()

    /** Updates the fields of an existing row, keeping its list membership. */
    @Query("UPDATE locations SET name = :name, type = :type, dimension = :dimension WHERE id = :id")
    suspend fun updateLocationFields(id: Int, name: String, type: String, dimension: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocationIfAbsent(location: LocationEntity)

    @Query("DELETE FROM location_residents WHERE locationId IN (:locationIds)")
    suspend fun deleteResidents(locationIds: List<Int>)

    @Insert
    suspend fun insertResidents(residents: List<LocationResidentEntity>)

    /** Atomically makes [locations] the whole list and resets the pagination metadata. */
    @Transaction
    suspend fun replaceFirstPage(
        locations: List<LocationEntity>,
        residents: List<LocationResidentEntity>,
        nextPage: Int?,
        totalPages: Int?
    ) {
        clearListMembership()
        savePage(locations, residents, nextPage, totalPages)
    }

    /** Atomically adds a following page to the list and advances the pagination metadata. */
    @Transaction
    suspend fun appendPage(
        locations: List<LocationEntity>,
        residents: List<LocationResidentEntity>,
        nextPage: Int?,
        totalPages: Int?
    ) {
        savePage(locations, residents, nextPage, totalPages)
    }

    /** Stores a location fetched for its detail screen without changing list membership. */
    @Transaction
    suspend fun saveDetail(location: LocationEntity, residents: List<LocationResidentEntity>) {
        val updated = updateLocationFields(location.id, location.name, location.type, location.dimension)
        if (updated == 0) insertLocationIfAbsent(location.copy(inList = false))
        replaceResidents(listOf(location.id), residents)
    }

    private suspend fun savePage(
        locations: List<LocationEntity>,
        residents: List<LocationResidentEntity>,
        nextPage: Int?,
        totalPages: Int?
    ) {
        upsertLocations(locations.map { it.copy(inList = true) })
        replaceResidents(locations.map { it.id }, residents)
        upsertPagingState(LocationPagingStateEntity(nextPage = nextPage, totalPages = totalPages))
    }

    private suspend fun replaceResidents(locationIds: List<Int>, residents: List<LocationResidentEntity>) {
        deleteResidents(locationIds)
        insertResidents(residents)
    }
}
