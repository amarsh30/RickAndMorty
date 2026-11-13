package com.amr.feature.character.favorite

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.amr.core.domain.model.Character
import com.amr.feature.character.detail.CharacterDetailContent
import com.amr.feature.character.detail.CharacterDetailUiState
import kotlin.test.Test
import kotlin.test.assertEquals

/** UI test for [FavoritesContent] and the favorite toggle on [CharacterDetailContent]. */
@OptIn(ExperimentalTestApi::class)
class FavoritesScreenTest {

    private val rick = Character(1, "Rick Sanchez", "Alive", "Human", null)

    @Test
    fun loading_showsProgress() = runComposeUiTest {
        setContent { FavoritesContent(FavoritesUiState(isLoading = true), onBack = {}, onCharacterClick = {}) }

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun empty_showsMessage() = runComposeUiTest {
        setContent { FavoritesContent(FavoritesUiState(isLoading = false), onBack = {}, onCharacterClick = {}) }

        onNodeWithText("Belum ada karakter favorit").assertIsDisplayed()
    }

    @Test
    fun error_showsMessage() = runComposeUiTest {
        setContent {
            FavoritesContent(
                FavoritesUiState(isLoading = false, errorMessage = "Gagal membaca favorit tersimpan."),
                onBack = {},
                onCharacterClick = {}
            )
        }

        onNodeWithText("Gagal membaca favorit tersimpan.").assertIsDisplayed()
    }

    /** Regression: the error button used to say "Coba Lagi" (retry) while it navigated back. */
    @Test
    fun error_actionIsLabelledAsTheNavigationItPerforms() = runComposeUiTest {
        var backs = 0
        setContent {
            FavoritesContent(
                FavoritesUiState(isLoading = false, errorMessage = "Gagal membaca favorit tersimpan."),
                onBack = { backs++ },
                onCharacterClick = {}
            )
        }

        onNodeWithText("Coba Lagi").assertDoesNotExist()
        onNodeWithText(BACK_TO_CHARACTERS_LABEL).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, backs)
    }

    @Test
    fun favorites_areListed_andClickOpensDetail() = runComposeUiTest {
        var clicked: Int? = null
        setContent {
            FavoritesContent(
                FavoritesUiState(isLoading = false, favorites = listOf(rick)),
                onBack = {},
                onCharacterClick = { clicked = it }
            )
        }

        onNodeWithText("Rick Sanchez").performClick()
        assertEquals(1, clicked)
    }

    @Test
    fun detail_favoriteButton_reflectsStateAndToggles() = runComposeUiTest {
        var toggles = 0
        var isFavorite by mutableStateOf(false)
        setContent {
            CharacterDetailContent(
                state = CharacterDetailUiState(isLoading = false, character = rick, isFavorite = isFavorite),
                onBack = {},
                onRetry = {},
                onToggleFavorite = { toggles++; isFavorite = !isFavorite }
            )
        }

        onNodeWithText("Tambah ke Favorit").performClick()
        onNodeWithText("Hapus dari Favorit").assertIsDisplayed()
        onNodeWithText("Hapus dari Favorit").performClick()
        onNodeWithText("Tambah ke Favorit").assertIsDisplayed()
        assertEquals(2, toggles)
    }

    @Test
    fun empty_ctaLeadsBackToCharacterList() = runComposeUiTest {
        var backs = 0
        setContent { FavoritesContent(FavoritesUiState(isLoading = false), onBack = { backs++ }, onCharacterClick = {}) }

        onNodeWithText(EXPLORE_CHARACTERS_LABEL).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, backs)
    }

    @Test
    fun favorites_reflectChangesImmediately() = runComposeUiTest {
        var state by mutableStateOf(FavoritesUiState(isLoading = false, favorites = listOf(rick)))
        setContent { FavoritesContent(state, onBack = {}, onCharacterClick = {}) }

        onNodeWithText("Rick Sanchez").assertIsDisplayed()
        // An unfavorite elsewhere arrives as a new list from the Room flow.
        state = state.copy(favorites = emptyList())
        onNodeWithText("Rick Sanchez").assertDoesNotExist()
        onNodeWithText("Belum ada karakter favorit").assertIsDisplayed()
    }

    @Test
    fun detail_showsInfoLocationsAndEpisodes() = runComposeUiTest {
        var openedLocation: Int? = null
        val full = rick.copy(
            gender = "Male",
            originName = "Earth (C-137)",
            originId = 1,
            locationName = "Citadel of Ricks",
            locationId = null,
            episodeIds = (1..30).toList()
        )
        setContent {
            CharacterDetailContent(
                state = CharacterDetailUiState(isLoading = false, character = full),
                onBack = {},
                onRetry = {},
                onToggleFavorite = {},
                onLocationClick = { openedLocation = it }
            )
        }

        onNodeWithText("Rick Sanchez").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        onNodeWithText("Male").assertExists()
        onNodeWithText("Ep 1").assertExists()
        onNodeWithText("+6 lainnya").assertExists()

        // Known origin is a tappable card; unknown-id location is plain text.
        onNode(hasClickAction() and hasText("Earth (C-137)")).performScrollTo().performClick()
        assertEquals(1, openedLocation)
        onNode(hasClickAction() and hasText("Citadel of Ricks")).assertDoesNotExist()
        onNodeWithText("Citadel of Ricks").assertExists()
    }

    @Test
    fun detail_refreshFailure_keepsDataAndOffersRetry() = runComposeUiTest {
        var retries = 0
        setContent {
            CharacterDetailContent(
                state = CharacterDetailUiState(isLoading = false, character = rick, errorMessage = "Gagal memperbarui data."),
                onBack = {},
                onRetry = { retries++ },
                onToggleFavorite = {}
            )
        }

        onNodeWithText("Gagal memperbarui data.").assertIsDisplayed()
        onNodeWithText("Rick Sanchez").assertIsDisplayed()
        onNodeWithText("Coba Lagi").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun detail_backArrow_isLabelledAndGoesBack() = runComposeUiTest {
        var backs = 0
        setContent {
            CharacterDetailContent(
                state = CharacterDetailUiState(isLoading = false, character = rick),
                onBack = { backs++ },
                onRetry = {},
                onToggleFavorite = {}
            )
        }

        onNodeWithContentDescription("Kembali").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, backs)
    }

    @Test
    fun favorites_backArrow_goesBack() = runComposeUiTest {
        var backs = 0
        setContent {
            FavoritesContent(
                FavoritesUiState(isLoading = false, favorites = listOf(rick)),
                onBack = { backs++ },
                onCharacterClick = {}
            )
        }

        onNodeWithText("Favorit").assertIsDisplayed()
        onNodeWithContentDescription("Kembali").performClick()
        assertEquals(1, backs)
    }
}
