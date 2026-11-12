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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.component.INFO_BANNER_TAG
import com.amr.core.designsystem.component.STATUS_CHIP_TAG
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.foundation.layout.width
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performScrollTo
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

    @Test
    fun clearButton_onlyWhenTextPresent_andClearsName() = runComposeUiTest {
        var filter by mutableStateOf(CharacterFilter())
        setContent { Content(CharacterSearchUiState(filter = filter), onFilterChange = { filter = it }) }

        onNodeWithContentDescription(CLEAR_NAME_LABEL).assertDoesNotExist()

        onNodeWithText("Nama").performTextInput("Rick")
        onNodeWithContentDescription(CLEAR_NAME_LABEL).assertHeightIsAtLeast(48.dp).performClick()

        assertEquals("", filter.name)
        onNodeWithContentDescription(CLEAR_NAME_LABEL).assertDoesNotExist()
    }

    @Test
    fun speciesFilter_hasItsOwnClearButton() = runComposeUiTest {
        var filter by mutableStateOf(CharacterFilter(name = "Rick", species = "Human"))
        setContent { Content(CharacterSearchUiState(filter = filter), onFilterChange = { filter = it }) }

        onNodeWithContentDescription(CLEAR_SPECIES_LABEL).performClick()

        assertEquals("", filter.species)
        assertEquals("Rick", filter.name)
    }

    @Test
    fun activeFilters_areListed_andEachIsRemovable() = runComposeUiTest {
        var filter by mutableStateOf(
            CharacterFilter(name = "Rick", status = CharacterFilter.Status.DEAD, gender = CharacterFilter.Gender.MALE)
        )
        setContent { Content(CharacterSearchUiState(filter = filter), onFilterChange = { filter = it }) }

        onNodeWithTag(ACTIVE_FILTERS_TAG).assertIsDisplayed()
        onNodeWithText("Status: Dead").assertIsDisplayed()
        onNodeWithText("Gender: Male").assertIsDisplayed()

        onNodeWithText("Gender: Male").performClick()
        assertEquals(null, filter.gender)
        assertEquals(CharacterFilter.Status.DEAD, filter.status)
        assertEquals("Rick", filter.name)

        onNodeWithText(CLEAR_ALL_FILTERS_LABEL).performClick()
        assertEquals(CharacterFilter(), filter)
        onNodeWithTag(ACTIVE_FILTERS_TAG).assertDoesNotExist()
    }

    @Test
    fun results_showSpeciesAndStatus_withOfflineBannerAboveThem() = runComposeUiTest {
        setContent {
            Content(
                CharacterSearchUiState(filter = CharacterFilter(name = "R"), results = listOf(rick), isOfflineResult = true)
            )
        }

        onNode(
            hasTestTag(INFO_BANNER_TAG) and
                hasAnyDescendant(hasText("Offline: hasil dari data tersimpan dan mungkin belum lengkap."))
        ).assertIsDisplayed()
        onNodeWithText("Human").assertIsDisplayed()
        onNode(hasTestTag(STATUS_CHIP_TAG) and hasText("Alive")).assertIsDisplayed()
    }

    @Test
    fun keyboardTab_movesFromNameFieldToSpeciesField() = runComposeUiTest {
        setContent { Content(CharacterSearchUiState()) }

        onNodeWithText("Nama").requestFocus().assertIsFocused()
        onNodeWithText("Nama").performKeyInput { pressKey(Key.Tab) }

        onNodeWithText("Spesies").assertIsFocused()
        onNodeWithText("Nama").assertIsNotFocused()
    }

    @Test
    fun chipRows_showScrollCueWhileMoreChipsAreOffscreen_andHideItAtTheEnd() = runComposeUiTest {
        var selectedStatus by mutableStateOf<CharacterFilter.Status?>(null)
        setContent {
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.width(260.dp)) {
                Content(
                    CharacterSearchUiState(filter = CharacterFilter(status = selectedStatus)),
                    onFilterChange = { selectedStatus = it.status }
                )
            }
        }

        // Narrow window: both rows overflow, so both show the cue.
        onAllNodesWithTag(CHIP_SCROLL_CUE_TAG).assertCountEquals(2)
        // The cue does not block taps on the chip under it.
        onNodeWithText("Dead").performClick()
        assertEquals(CharacterFilter.Status.DEAD, selectedStatus)

        // Scrolled to the end, each row's cue disappears.
        onNodeWithText("Status unknown").performScrollTo()
        onNodeWithText("Gender unknown").performScrollTo()
        waitForIdle()
        onAllNodesWithTag(CHIP_SCROLL_CUE_TAG).assertCountEquals(0)
        // Filter state survives the scrolling.
        onNodeWithText("Dead").assertIsSelected()
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
