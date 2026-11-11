package com.amr.feature.character.favorite

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithText
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
}
