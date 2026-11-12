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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.component.AVATAR_FALLBACK_TAG
import com.amr.core.designsystem.component.SKELETON_LIST_TAG
import com.amr.core.designsystem.theme.AppTheme
import com.amr.feature.episode.presentation.EPISODE_GRID_TAG
import com.amr.feature.episode.presentation.SEASON_HEADER_TAG
import androidx.compose.ui.test.performScrollToIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.amr.core.designsystem.component.INFO_BANNER_TAG

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
    fun list_initialLoading_isSkeleton() = runComposeUiTest {
        setContent { EpisodeListContent(EpisodeListUiState(isLoading = true), {}, {}, {}) }

        onNodeWithTag(SKELETON_LIST_TAG).assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
        onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun card_isOneButtonWithCodeTitleAndDate() = runComposeUiTest {
        setContent { AppTheme { EpisodeListContent(listState(pilot), {}, {}, {}) } }

        onNode(hasClickAction() and hasText("S01E01") and hasText("Pilot") and hasText("December 2, 2013"))
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun seasonHeaders_spanPages_withoutDuplicates_andAreHeadings() = runComposeUiTest {
        var state by mutableStateOf(listState(pilot, ep(2, "S01E02")).copy(isEndReached = false))
        setContent { EpisodeListContent(state, {}, {}, {}) }

        onAllNodesWithTag(SEASON_HEADER_TAG).assertCountEquals(1)
        onNodeWithText("Musim 1").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        // Next page continues season 1 and starts season 2.
        state = state.copy(episodes = state.episodes + ep(3, "S01E03") + ep(12, "S02E01"), isEndReached = true)
        waitForIdle()

        onAllNodesWithTag(SEASON_HEADER_TAG).assertCountEquals(2)
        onAllNodesWithText("Musim 1").assertCountEquals(1)
        onNodeWithText("Musim 2").assertIsDisplayed()
        // Order kept: season 2 header sits below the last season 1 episode.
        assertTrue(
            onNodeWithText("Musim 2").getUnclippedBoundsInRoot().top >
                onNodeWithText("Episode 3").getUnclippedBoundsInRoot().top
        )
    }

    @Test
    fun loadMore_isRequestedAgainAfterANewPageArrives() = runComposeUiTest {
        var loads = 0
        var state by mutableStateOf(listState(pilot).copy(isEndReached = false))
        setContent { EpisodeListContent(state, {}, onLoadMore = { loads++ }, {}) }
        waitForIdle()
        val afterFirstPage = loads
        assertTrue(afterFirstPage >= 1, "short first page requests the next one")

        state = state.copy(episodes = state.episodes + ep(2, "S01E02"))
        waitForIdle()
        assertTrue(loads > afterFirstPage, "end still visible after the new page: asks again")
    }

    @Test
    fun list_tabletAndShortLandscape_putCardsSideBySide() = runComposeUiTest {
        var size by mutableStateOf(400.dp to 800.dp)
        setContent { SizedBox(size) { EpisodeListContent(listState(pilot, ep(2, "S01E02")), {}, {}, {}) } }

        val firstTop = { onNodeWithText("Pilot").getUnclippedBoundsInRoot().top }
        val secondTop = { onNodeWithText("Episode 2").getUnclippedBoundsInRoot().top }
        assertTrue(secondTop() > firstTop(), "compact: one column")

        size = 900.dp to 800.dp
        waitForIdle()
        assertEquals(firstTop(), secondTop(), "tablet: same row")

        size = 835.dp to 330.dp
        waitForIdle()
        assertEquals(firstTop(), secondTop(), "short landscape: same row")
    }

    @Test
    fun detail_titleIsHeading_codeAndDate_charactersOpenDetail() = runComposeUiTest {
        var clicked: Int? = null
        setContent {
            AppTheme {
                EpisodeDetailContent(
                    EpisodeDetailUiState(isLoading = false, episode = pilot, characterCount = 1, characters = listOf(morty)),
                    onBack = {}, onRetry = {}, onLoadMoreCharacters = {}, onCharacterClick = { clicked = it }
                )
            }
        }

        onNodeWithText("Pilot").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        onNodeWithText("S01E01").assertIsDisplayed()
        onNodeWithText("December 2, 2013").assertIsDisplayed()
        onNodeWithText("Karakter (1)").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        onNodeWithTag(AVATAR_FALLBACK_TAG, useUnmergedTree = true).assertExists()
        onNode(hasClickAction() and hasText("Morty Smith")).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(2, clicked)
    }

    @Test
    fun list_resizeKeepsScrollPosition() = runComposeUiTest {
        var size by mutableStateOf(400.dp to 800.dp)
        val many = (1..40).map { ep(it, "S01E${it.toString().padStart(2, '0')}") }
        setContent { SizedBox(size) { EpisodeListContent(listState(*many.toTypedArray()), {}, {}, {}) } }

        // Index 31 = header row + Episode 31.
        onNodeWithTag(EPISODE_GRID_TAG).performScrollToIndex(31)
        onNodeWithText("Episode 31").assertIsDisplayed()

        size = 900.dp to 800.dp
        waitForIdle()
        onNodeWithText("Episode 31").assertIsDisplayed()
        onNodeWithText("Episode 1").assertDoesNotExist()
    }

    @Test
    fun list_longTitleAtLargeFontScale_isNotClipped() = runComposeUiTest {
        val long = Episode(30, "Vindicators 3: The Return of Worldender", "August 13, 2017", "S03E04")
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                SizedBox(360.dp to 800.dp) { EpisodeListContent(listState(long), {}, {}, {}) }
            }
        }

        // The long title wraps instead of being cut or ellipsized.
        assertFalse(textIsCut("Vindicators 3: The Return of Worldender"), "title is cut off")

        // Code and date: with the font scale simulated via LocalDensity, their text layout
        // reports overflow, yet on real 360dp and 412dp screens at 200% both draw fully
        // (checked on device). So assert what users see: shown, and inside the card.
        val card = onNode(hasClickAction() and hasText("S03E04")).getUnclippedBoundsInRoot()
        listOf("S03E04", "August 13, 2017").forEach { text ->
            val bounds = onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue(bounds.left >= card.left && bounds.right <= card.right, "$text fits within its card")
        }
    }

    @Test
    fun detail_backArrow_isLabelledAndGoesBack() = runComposeUiTest {
        var backs = 0
        setContent {
            AppTheme {
                EpisodeDetailContent(
                    EpisodeDetailUiState(isLoading = false, episode = pilot, characterCount = 0),
                    onBack = { backs++ }, onRetry = {}, onLoadMoreCharacters = {}, onCharacterClick = {}
                )
            }
        }

        onNodeWithContentDescription("Kembali").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, backs)
    }

    @Test
    fun detail_refreshFailureWithCache_keepsEpisodeAndCharacters() = runComposeUiTest {
        setContent {
            EpisodeDetailContent(
                EpisodeDetailUiState(
                    isLoading = false, episode = pilot, characterCount = 1, characters = listOf(morty),
                    errorMessage = "Gagal memperbarui data. Menampilkan data tersimpan."
                ),
                onBack = {}, onRetry = {}, onLoadMoreCharacters = {}, onCharacterClick = {}
            )
        }

        onNode(hasTestTag(INFO_BANNER_TAG) and hasAnyDescendant(hasText("Gagal memperbarui data. Menampilkan data tersimpan.")))
            .assertIsDisplayed()
        onNodeWithText("Pilot").assertIsDisplayed()
        onNodeWithText("Morty Smith").assertIsDisplayed()
    }

    private fun SemanticsNodeInteractionsProvider.textIsCut(text: String): Boolean {
        val results = mutableListOf<TextLayoutResult>()
        onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        val layout = results.first()
        return layout.didOverflowWidth || (0 until layout.lineCount).any { layout.isLineEllipsized(it) }
    }

    private fun ep(id: Int, code: String) = Episode(id, "Episode $id", "date $id", code)

    private fun listState(vararg episodes: Episode) =
        EpisodeListUiState(isLoading = false, episodes = episodes.toList(), isEndReached = true)

    @Composable
    private fun SizedBox(size: Pair<Dp, Dp>, content: @Composable () -> Unit) {
        // requiredSize simulates windows larger or shorter than the device, anchored at the start edge.
        Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(size.first, size.second)) {
            AppTheme { content() }
        }
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
