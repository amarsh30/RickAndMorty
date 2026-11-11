package com.amr.core.domain.usecase

import com.amr.core.domain.repository.EpisodeRepository

class ObserveEpisodesUseCase(private val repository: EpisodeRepository) {
    operator fun invoke() = repository.observeEpisodes()
}

class ObserveEpisodePaginationStateUseCase(private val repository: EpisodeRepository) {
    operator fun invoke() = repository.observePaginationState()
}

class RefreshEpisodesUseCase(private val repository: EpisodeRepository) {
    suspend operator fun invoke() = repository.refreshEpisodes()
}

class LoadNextEpisodesPageUseCase(private val repository: EpisodeRepository) {
    suspend operator fun invoke() = repository.loadNextPage()
}

class ObserveEpisodeDetailUseCase(private val repository: EpisodeRepository) {
    operator fun invoke(id: Int) = repository.observeEpisode(id)
}

class ObserveEpisodeCharacterIdsUseCase(private val repository: EpisodeRepository) {
    operator fun invoke(id: Int) = repository.observeCharacterIds(id)
}

class RefreshEpisodeDetailUseCase(private val repository: EpisodeRepository) {
    suspend operator fun invoke(id: Int) = repository.refreshEpisode(id)
}
