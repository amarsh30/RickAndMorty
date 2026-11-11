package com.amr.feature.episode

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.Episode
import com.amr.feature.episode.detail.EpisodeDetailContent
import com.amr.feature.episode.detail.EpisodeDetailUiState
import com.amr.feature.episode.presentation.EpisodeListContent
import com.amr.feature.episode.presentation.EpisodeListUiState
import kotlin.test.Test
import kotlin.test.assertEquals

/** UI test for the episode screens, driven by fixed UI states. */
@OptIn(ExperimentalTestApi::class)
class EpisodeScreensTest {

    private val pilot = Episode(1, "Pilot", "December 2, 2013", "S01E01")
    private val morty = Character(2, "Morty Smith", "Alive", "Human", null)

    @Test
    fun list_showsCodeNameAndAirDate_andClickOpensDetail() = runComposeUiTest {
        var clicked: Int? = null
        setContent {
            EpisodeListContent(
                EpisodeListUiState(isLoading = false, episodes = listOf(pilot)),
                onRetry = {}, onLoadMore = {}, onEpisodeClick = { clicked = it }
            )
        }

        onNodeWithText("S01E01").assertIsDisplayed()
        onNodeWithText("December 2, 2013").assertIsDisplayed()
        onNodeWithText("Pilot").performClick()
        assertEquals(1, clicked)
    }

    @Test
    fun list_cachedWithStaleWarning_keepsList() = runComposeUiTest {
        setContent {
            EpisodeListContent(
                EpisodeListUiState(
                    isLoading = false, episodes = listOf(pilot),
                    errorMessage = "Gagal memperbarui data. Menampilkan data tersimpan."
                ),
                onRetry = {}, onLoadMore = {}, onEpisodeClick = {}
            )
        }

        onNodeWithText("Gagal memperbarui data. Menampilkan data tersimpan.").assertIsDisplayed()
        onNodeWithText("Pilot").assertIsDisplayed()
    }

    @Test
    fun list_empty_showsEmptyState() = runComposeUiTest {
        setContent { EpisodeListContent(EpisodeListUiState(isLoading = false), {}, {}, {}) }

        onNodeWithText("Belum ada episode").assertIsDisplayed()
    }

    @Test
    fun detail_showsCharacters_andClickOpensCharacter() = runComposeUiTest {
        var clicked: Int? = null
        setContent {
            EpisodeDetailContent(
                EpisodeDetailUiState(isLoading = false, episode = pilot, characterCount = 19, characters = listOf(morty)),
                onBack = {}, onRetry = {}, onLoadMoreCharacters = {}, onCharacterClick = { clicked = it }
            )
        }

        onNodeWithText("Karakter (19)").assertIsDisplayed()
        onNodeWithText("Morty Smith").performClick()
        assertEquals(2, clicked)
    }

    @Test
    fun detail_characterFailure_showsRetry() = runComposeUiTest {
        var calls = 0
        setContent {
            EpisodeDetailContent(
                EpisodeDetailUiState(
                    isLoading = false, episode = pilot, characterCount = 19,
                    charactersErrorMessage = "Gagal memuat episode. Periksa koneksi internet.",
                    canShowMoreCharacters = true
                ),
                onBack = {}, onRetry = {}, onLoadMoreCharacters = { calls++ }, onCharacterClick = {}
            )
        }
        val before = calls

        onNodeWithText("Muat Lagi").performClick()
        assertEquals(before + 1, calls)
    }
}
