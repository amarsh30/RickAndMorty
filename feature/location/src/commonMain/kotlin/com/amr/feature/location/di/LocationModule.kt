package com.amr.feature.location.di

import com.amr.feature.location.detail.LocationDetailViewModel
import com.amr.feature.location.presentation.LocationListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val locationModule = module {
    viewModelOf(::LocationListViewModel)
    viewModel { (locationId: Int) ->
        LocationDetailViewModel(
            locationId = locationId,
            observeLocationDetail = get(),
            observeResidentIds = get(),
            refreshLocationDetail = get(),
            observeRelatedCharacters = get(),
            fetchRelatedCharacters = get(),
            connectivityRestored = get()
        )
    }
}
