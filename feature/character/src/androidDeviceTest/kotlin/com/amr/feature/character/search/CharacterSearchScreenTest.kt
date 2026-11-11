package com.amr.feature.character.search

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.CharacterFilter
import kotlin.test.Test
import kotlin.test.assertEquals

/** UI test for [CharacterSearchContent], driven by fixed UI states. */
@OptIn(ExperimentalTestApi::class)
class CharacterSearchScreenTest {

    private val rick = Character(1, "Rick Sanchez", "Alive", "Human", null)
    private val morty = Character(2, "Morty Smith", "Alive", "Human", null)

    @Test
    fun emptyFilter_showsHint() = runComposeUiTest {
        setContent { Content(CharacterSearchUiState()) }

        onNodeWithText("Masukkan nama, spesies, status, atau gender.").assertIsDisplayed()
    }

    @Test
    fun searchingRemote_withoutResults_showsProgressNotEmptyMessage() = runComposeUiTest {
        setContent {
            Content(CharacterSearchUiState(filter = CharacterFilter(name = "Rick"), isSearchingRemote = true))
        }

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
        onNodeWithText("Tidak ada karakter yang cocok.").assertDoesNotExist()
    }

    @Test
    fun noMatch_showsEmptyMessage() = runComposeUiTest {
        setContent { Content(CharacterSearchUiState(filter = CharacterFilter(name = "Zzz"))) }

        onNodeWithText("Tidak ada karakter yang cocok.").assertIsDisplayed()
    }

    @Test
    fun results_areListed_andClickOpensCharacter() = runComposeUiTest {
        var clicked: Int? = null
        setContent {
            Content(
                CharacterSearchUiState(filter = CharacterFilter(name = "S"), results = listOf(rick, morty)),
                onCharacterClick = { clicked = it }
            )
        }

        onNodeWithText("Rick Sanchez").assertIsDisplayed()
        onNodeWithText("Morty Smith").performClick()
        assertEquals(2, clicked)
    }

    @Test
    fun offline_showsIncompleteNotice_andRetry() = runComposeUiTest {
        var retries = 0
        setContent {
            Content(
                CharacterSearchUiState(
                    filter = CharacterFilter(name = "Rick"),
                    results = listOf(rick),
                    isOfflineResult = true
                ),
                onRetry = { retries++ }
            )
        }

        onNodeWithText("Offline: hasil dari data tersimpan dan mungkin belum lengkap.").assertIsDisplayed()
        onNodeWithText("Rick Sanchez").assertIsDisplayed()
        onNodeWithText("Coba Lagi").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun error_isShown() = runComposeUiTest {
        setContent {
            Content(CharacterSearchUiState(filter = CharacterFilter(name = "Rick"), errorMessage = "Server error"))
        }

        onNodeWithText("Server error").assertIsDisplayed()
    }

    @Test
    fun typingAndChips_updateFilter() = runComposeUiTest {
        var filter = CharacterFilter()
        setContent { Content(CharacterSearchUiState(filter = CharacterFilter(status = CharacterFilter.Status.DEAD)), onFilterChange = { filter = it }) }

        onNodeWithText("Dead").assertIsSelected()

        onNodeWithText("Alive").performClick()
        assertEquals(CharacterFilter.Status.ALIVE, filter.status)

        // Tapping the selected chip clears it.
        onNodeWithText("Dead").performClick()
        assertEquals(null, filter.status)

        onNodeWithText("Nama").performTextInput("Rick")
        assertEquals("Rick", filter.name)
    }

    @androidx.compose.runtime.Composable
    private fun Content(
        state: CharacterSearchUiState,
        onFilterChange: (CharacterFilter) -> Unit = {},
        onRetry: () -> Unit = {},
        onCharacterClick: (Int) -> Unit = {}
    ) = CharacterSearchContent(
        state = state,
        onBack = {},
        onFilterChange = onFilterChange,
        onRetry = onRetry,
        onCharacterClick = onCharacterClick
    )
}
