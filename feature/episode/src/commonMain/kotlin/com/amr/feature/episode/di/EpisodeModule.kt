package com.amr.feature.episode.di

import com.amr.feature.episode.detail.EpisodeDetailViewModel
import com.amr.feature.episode.presentation.EpisodeListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val episodeModule = module {
    viewModelOf(::EpisodeListViewModel)
    viewModel { (episodeId: Int) ->
        EpisodeDetailViewModel(
            episodeId = episodeId,
            observeEpisodeDetail = get(),
            observeCharacterIds = get(),
            refreshEpisodeDetail = get(),
            observeRelatedCharacters = get(),
            fetchRelatedCharacters = get()
        )
    }
}
