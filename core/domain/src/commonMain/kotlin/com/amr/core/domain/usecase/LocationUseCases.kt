package com.amr.core.domain.usecase

import com.amr.core.domain.repository.LocationRepository

class ObserveLocationsUseCase(private val repository: LocationRepository) {
    operator fun invoke() = repository.observeLocations()
}

class ObserveLocationPaginationStateUseCase(private val repository: LocationRepository) {
    operator fun invoke() = repository.observePaginationState()
}

class RefreshLocationsUseCase(private val repository: LocationRepository) {
    suspend operator fun invoke() = repository.refreshLocations()
}

class LoadNextLocationsPageUseCase(private val repository: LocationRepository) {
    suspend operator fun invoke() = repository.loadNextPage()
}

class ObserveLocationDetailUseCase(private val repository: LocationRepository) {
    operator fun invoke(id: Int) = repository.observeLocation(id)
}

class ObserveLocationResidentIdsUseCase(private val repository: LocationRepository) {
    operator fun invoke(id: Int) = repository.observeResidentIds(id)
}

class RefreshLocationDetailUseCase(private val repository: LocationRepository) {
    suspend operator fun invoke(id: Int) = repository.refreshLocation(id)
}
