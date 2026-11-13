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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.material3.MaterialTheme
import com.amr.core.designsystem.theme.AppTheme
import kotlin.test.assertNotEquals
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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

    // --- Search semantics (6C.2A) ---

    @Test
    fun remoteSearchProgress_isLabelled() = runComposeUiTest {
        setContent {
            Content(CharacterSearchUiState(filter = CharacterFilter(name = "Rick"), isSearchingRemote = true))
        }

        onNodeWithContentDescription(SEARCHING_LABEL)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
    }

    @Test
    fun resultCount_isAPoliteLiveRegion_thatFollowsTheResults() = runComposeUiTest {
        var state by mutableStateOf(
            CharacterSearchUiState(filter = CharacterFilter(name = "S"), results = listOf(rick, morty))
        )
        setContent { Content(state) }

        onNodeWithTag(SEARCH_RESULT_COUNT_TAG)
            .assert(hasText(searchResultCountText(2)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))

        state = state.copy(results = listOf(rick))
        onNodeWithTag(SEARCH_RESULT_COUNT_TAG).assert(hasText(searchResultCountText(1)))
    }

    @Test
    fun resultCount_isNotShownWithoutAQueryOrWithoutResults() = runComposeUiTest {
        var state by mutableStateOf(CharacterSearchUiState())
        setContent { Content(state) }
        onNodeWithTag(SEARCH_RESULT_COUNT_TAG).assertDoesNotExist()

        state = CharacterSearchUiState(filter = CharacterFilter(name = "Zzz"))
        onNodeWithTag(SEARCH_RESULT_COUNT_TAG).assertDoesNotExist()
    }

    @Test
    fun noResults_isOneMergedPoliteLiveRegion() = runComposeUiTest {
        setContent { Content(CharacterSearchUiState(filter = CharacterFilter(name = "Zzz"))) }

        onNodeWithTag(NO_RESULTS_TAG)
            .assert(hasText("Tidak ada karakter yang cocok.") and hasText("Coba ubah atau hapus sebagian filter."))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
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
        // Each active filter is one button named by the filter it removes.
        onNodeWithContentDescription(removeFilterLabel("Status: Dead")).assertIsDisplayed()
        onNodeWithContentDescription(removeFilterLabel("Gender: Male")).assertIsDisplayed()

        onNodeWithContentDescription(removeFilterLabel("Gender: Male")).performClick()
        assertEquals(null, filter.gender)
        assertEquals(CharacterFilter.Status.DEAD, filter.status)
        assertEquals("Rick", filter.name)

        onNodeWithText(CLEAR_ALL_FILTERS_LABEL).performClick()
        assertEquals(CharacterFilter(), filter)
        onNodeWithTag(ACTIVE_FILTERS_TAG).assertDoesNotExist()
    }

    // --- Filter semantics and focus (6C.2B) ---

    @Test
    fun filterChips_namedWithTheirGroup_inSelectableGroups_withSelectedAndUnselectedStates() = runComposeUiTest {
        var filter by mutableStateOf(CharacterFilter(status = CharacterFilter.Status.DEAD))
        setContent { Content(CharacterSearchUiState(filter = filter), onFilterChange = { filter = it }) }

        onNodeWithContentDescription(filterChipName(STATUS_GROUP_LABEL, "Dead")).assertIsSelected()
        onNodeWithContentDescription(filterChipName(STATUS_GROUP_LABEL, "Alive")).assertIsNotSelected()
        onNodeWithContentDescription(filterChipName(GENDER_GROUP_LABEL, "Male")).assertIsNotSelected()
        // Each chip row is a selectable group (one choice per group).
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)).assertCountEquals(2)

        // Behaviour unchanged: choosing another option replaces the choice.
        onNodeWithContentDescription(filterChipName(STATUS_GROUP_LABEL, "Alive")).performClick()
        assertEquals(CharacterFilter.Status.ALIVE, filter.status)
    }

    @Test
    fun activeFilterChip_isARemoveButton_notASelectedItem() = runComposeUiTest {
        var filter by mutableStateOf(CharacterFilter(name = "Rick"))
        setContent { Content(CharacterSearchUiState(filter = filter), onFilterChange = { filter = it }) }

        onNodeWithContentDescription(removeFilterLabel("Nama: Rick"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals("", filter.name)
        onNodeWithTag(ACTIVE_FILTERS_TAG).assertDoesNotExist()
    }

    @Test
    fun activeFilterChip_andClearAll_showFocusRingOnlyWithKeyboardFocus() = runComposeUiTest {
        var primary = Color.Unspecified
        lateinit var inputMode: InputModeManager
        setContent {
            inputMode = LocalInputModeManager.current
            AppTheme {
                primary = MaterialTheme.colorScheme.primary
                Content(CharacterSearchUiState(filter = CharacterFilter(name = "Rick", status = CharacterFilter.Status.DEAD)))
            }
        }
        fun SemanticsNodeInteraction.edgePixel(): Color {
            val map = captureToImage().toPixelMap()
            return map[1, map.height / 2]
        }
        val chip = onNodeWithContentDescription(removeFilterLabel("Nama: Rick"))
        val clearAll = onNodeWithText(CLEAR_ALL_FILTERS_LABEL)

        // Touch mode: no ring.
        assertNotEquals(primary, chip.edgePixel())
        assertNotEquals(primary, clearAll.edgePixel())

        // Keyboard mode (in touch mode Compose clickables are not focusable).
        runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        chip.requestFocus()
        waitForIdle()
        assertEquals(primary, chip.edgePixel(), "active-filter chip ring while focused")

        clearAll.requestFocus()
        waitForIdle()
        assertEquals(primary, clearAll.edgePixel(), "clear-all ring while focused")
        assertNotEquals(primary, chip.edgePixel(), "ring follows focus")
        clearAll.assertHeightIsAtLeast(48.dp)
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
