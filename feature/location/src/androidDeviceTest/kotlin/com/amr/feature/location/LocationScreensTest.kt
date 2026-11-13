package com.amr.feature.location

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.Location
import com.amr.feature.location.detail.LocationDetailContent
import com.amr.feature.location.detail.LocationDetailUiState
import com.amr.feature.location.detail.RESIDENT_LOAD_MORE_INDICATOR_TAG
import com.amr.feature.location.presentation.LocationListContent
import com.amr.feature.location.presentation.LocationListUiState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.component.AVATAR_FALLBACK_TAG
import com.amr.core.designsystem.component.INFO_BANNER_TAG
import com.amr.core.designsystem.component.REFRESH_LABEL
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import com.amr.core.designsystem.component.SKELETON_LIST_TAG
import com.amr.core.designsystem.theme.AppTheme
import com.amr.feature.location.presentation.LOCATION_GRID_TAG
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** UI test for the location screens, driven by fixed UI states. */
@OptIn(ExperimentalTestApi::class)
class LocationScreensTest {

    private val earth = Location(1, "Earth (C-137)", "Planet", "Dimension C-137")
    private val citadel = Location(3, "Citadel of Ricks", "Space station", "unknown")
    private val rick = Character(1, "Rick Sanchez", "Alive", "Human", null)

    // --- list ---

    @Test
    fun list_loading_showsProgress() = runComposeUiTest {
        setContent { LocationListContent(LocationListUiState(isLoading = true), {}, {}, {}) }

        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun list_emptyCache_withError_showsReasonAndRetry() = runComposeUiTest {
        var retries = 0
        setContent {
            LocationListContent(
                LocationListUiState(isLoading = false, errorMessage = "Gagal memuat lokasi. Periksa koneksi internet."),
                onRetry = { retries++ }, onLoadMore = {}, onLocationClick = {}
            )
        }

        onNodeWithText("Gagal memuat lokasi. Periksa koneksi internet.").assertIsDisplayed()
        onNodeWithText("Coba Lagi").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun list_empty_withoutError_showsEmptyState() = runComposeUiTest {
        setContent { LocationListContent(LocationListUiState(isLoading = false), {}, {}, {}) }

        onNodeWithText("Belum ada lokasi").assertIsDisplayed()
    }

    @Test
    fun list_showsLocations_andClickOpensDetail() = runComposeUiTest {
        var clicked: Int? = null
        setContent {
            LocationListContent(
                LocationListUiState(isLoading = false, locations = listOf(earth, citadel), isEndReached = true),
                onRetry = {}, onLoadMore = {}, onLocationClick = { clicked = it }
            )
        }

        onNodeWithText("Planet · Dimension C-137").assertIsDisplayed()
        onNodeWithText("Semua lokasi sudah ditampilkan").assertIsDisplayed()
        onNodeWithText("Citadel of Ricks").performClick()
        assertEquals(3, clicked)
    }

    @Test
    fun list_loadMoreFailure_showsFooterRetry() = runComposeUiTest {
        var loadMoreCalls = 0
        setContent {
            LocationListContent(
                LocationListUiState(isLoading = false, locations = listOf(earth), loadMoreErrorMessage = "Timeout"),
                onRetry = {}, onLoadMore = { loadMoreCalls++ }, onLocationClick = {}
            )
        }
        val before = loadMoreCalls

        onNodeWithText("Timeout").assertIsDisplayed()
        onNodeWithText("Muat Lagi").performClick()
        assertEquals(before + 1, loadMoreCalls)
    }

    @Test
    fun list_initialLoading_isSkeletonExposedAsOneLoadingNode() = runComposeUiTest {
        setContent { LocationListContent(LocationListUiState(isLoading = true), {}, {}, {}) }

        onNodeWithTag(SKELETON_LIST_TAG).assertIsDisplayed()
        onNodeWithContentDescription("Memuat").assertIsDisplayed()
        onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun card_isOneLabelledButtonWithNameAndMetadata() = runComposeUiTest {
        setContent { Themed { LocationListContent(listState(earth), {}, {}, {}) } }

        onNode(hasClickAction() and hasText("Earth (C-137)") and hasText("Planet · Dimension C-137"))
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun list_refreshFailureWithCache_isInlineBanner() = runComposeUiTest {
        setContent {
            LocationListContent(
                listState(earth).copy(errorMessage = "Gagal memperbarui data. Menampilkan data tersimpan."),
                {}, {}, {}
            )
        }

        onNode(hasTestTag(INFO_BANNER_TAG) and hasAnyDescendant(hasText("Gagal memperbarui data. Menampilkan data tersimpan.")))
            .assertIsDisplayed()
        onNodeWithText("Earth (C-137)").assertIsDisplayed()
    }

    @Test
    fun list_refreshAction_isLabelledButton_reusesRefresh_andIsDisabledWhileRefreshing() = runComposeUiTest {
        var state by mutableStateOf(listState(earth))
        var refreshes = 0
        setContent { LocationListContent(state, onRetry = { refreshes++ }, onLoadMore = {}, onLocationClick = {}) }

        onNodeWithContentDescription(REFRESH_LABEL)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .assertIsEnabled()
            .performClick()
        assertEquals(1, refreshes)

        state = state.copy(isRefreshing = true)
        onNodeWithContentDescription(REFRESH_LABEL).assertIsNotEnabled().performClick()
        assertEquals(1, refreshes)

        state = state.copy(isRefreshing = false)
        onNodeWithContentDescription(REFRESH_LABEL).assertIsEnabled().performClick()
        assertEquals(2, refreshes)
    }

    @Test
    fun list_refreshAction_isHiddenWhileLoadingOrEmpty() = runComposeUiTest {
        var state by mutableStateOf(LocationListUiState(isLoading = true))
        setContent { LocationListContent(state, {}, {}, {}) }
        onNodeWithContentDescription(REFRESH_LABEL).assertDoesNotExist()

        state = LocationListUiState(isLoading = false)
        onNodeWithContentDescription(REFRESH_LABEL).assertDoesNotExist()
    }

    @Test
    fun list_compactIsOneColumn_wideAndShortLandscapeShareRows() = runComposeUiTest {
        var size by mutableStateOf(400.dp to 800.dp)
        setContent { SizedBox(size) { LocationListContent(listState(earth, citadel), {}, {}, {}) } }

        val earthTop = { onNodeWithText("Earth (C-137)").getUnclippedBoundsInRoot().top }
        val citadelTop = { onNodeWithText("Citadel of Ricks").getUnclippedBoundsInRoot().top }
        assertTrue(citadelTop() > earthTop(), "compact: one column")

        size = 900.dp to 800.dp
        waitForIdle()
        assertEquals(earthTop(), citadelTop(), "tablet: same row")

        // Phone landscape: short height, still horizontal cards side by side.
        size = 835.dp to 330.dp
        waitForIdle()
        assertEquals(earthTop(), citadelTop(), "short landscape: same row")
        onNode(hasClickAction() and hasText("Citadel of Ricks")).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun list_resizeKeepsScrollPosition() = runComposeUiTest {
        var size by mutableStateOf(400.dp to 800.dp)
        val many = (1..40).map { Location(it, "Location $it", "Planet", "Dim $it") }
        setContent { SizedBox(size) { LocationListContent(listState(*many.toTypedArray()), {}, {}, {}) } }

        onNodeWithTag(LOCATION_GRID_TAG).performScrollToIndex(30)
        onNodeWithText("Location 31").assertIsDisplayed()

        size = 900.dp to 800.dp
        waitForIdle()
        onNodeWithText("Location 31").assertIsDisplayed()
        onNodeWithText("Location 1").assertDoesNotExist()
    }

    @Test
    fun list_largeFontScale_doesNotClipNameOrMetadata() = runComposeUiTest {
        val long = Location(9, "Interdimensional Customs Checkpoint", "Space station", "Replacement Dimension")
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                SizedBox(360.dp to 800.dp) { LocationListContent(listState(long), {}, {}, {}) }
            }
        }

        assertFalse(textOverflows("Interdimensional Customs Checkpoint"))
        assertFalse(textOverflows("Space station · Replacement Dimension"))
    }

    // --- detail ---

    @Test
    fun detail_nameIsHeading_residentsUseAvatarRowsThatOpenCharacter() = runComposeUiTest {
        var clicked: Int? = null
        setContent {
            Themed {
                LocationDetailContent(
                    LocationDetailUiState(isLoading = false, location = citadel, residentCount = 1, residents = listOf(rick)),
                    onBack = {}, onRetry = {}, onLoadMoreResidents = {}, onCharacterClick = { clicked = it }
                )
            }
        }

        onNodeWithText("Citadel of Ricks").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        onNodeWithText("Penghuni (1)").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        onNodeWithText("Alive · Human").assertIsDisplayed()
        onNodeWithTag(AVATAR_FALLBACK_TAG, useUnmergedTree = true).assertExists()
        onNode(hasClickAction() and hasText("Rick Sanchez")).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, clicked)
    }

    @Test
    fun detail_residentWindowFailure_retriesOnceWithoutAutoRetrying() = runComposeUiTest {
        var loads = 0
        setContent {
            LocationDetailContent(
                LocationDetailUiState(
                    isLoading = false, location = citadel, residentCount = 101, residents = listOf(rick),
                    residentsErrorMessage = "Timeout", canShowMoreResidents = true
                ),
                onBack = {}, onRetry = {}, onLoadMoreResidents = { loads++ }, onCharacterClick = {}
            )
        }
        waitForIdle()
        val before = loads
        waitForIdle()
        // A failed window is not retried by itself...
        assertEquals(before, loads)

        // ...only by the footer button, once per tap.
        onNodeWithText("Timeout").assertIsDisplayed()
        onNodeWithText("Muat Lagi").performClick()
        assertEquals(before + 1, loads)
    }

    @Test
    fun detail_refreshFailure_keepsDataWithBanner() = runComposeUiTest {
        setContent {
            LocationDetailContent(
                LocationDetailUiState(isLoading = false, location = citadel, residentCount = 0, errorMessage = "Gagal memperbarui data."),
                onBack = {}, onRetry = {}, onLoadMoreResidents = {}, onCharacterClick = {}
            )
        }

        onNode(hasTestTag(INFO_BANNER_TAG) and hasAnyDescendant(hasText("Gagal memperbarui data."))).assertIsDisplayed()
        onNodeWithText("Space station").assertIsDisplayed()
    }

    @Test
    fun detail_backArrow_isLabelledAndGoesBack() = runComposeUiTest {
        var backs = 0
        setContent {
            Themed {
                LocationDetailContent(
                    LocationDetailUiState(isLoading = false, location = earth, residentCount = 0),
                    onBack = { backs++ }, onRetry = {}, onLoadMoreResidents = {}, onCharacterClick = {}
                )
            }
        }

        onNodeWithContentDescription("Kembali").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, backs)
    }

    private fun listState(vararg locations: Location) =
        LocationListUiState(isLoading = false, locations = locations.toList(), isEndReached = true)

    private fun SemanticsNodeInteractionsProvider.textOverflows(text: String): Boolean {
        val results = mutableListOf<TextLayoutResult>()
        onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first().hasVisualOverflow
    }

    @Composable
    private fun Themed(content: @Composable () -> Unit) = AppTheme { content() }

    @Composable
    private fun SizedBox(size: Pair<Dp, Dp>, content: @Composable () -> Unit) {
        // requiredSize simulates windows larger or shorter than the device, anchored at the start edge.
        Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(size.first, size.second)) {
            AppTheme { content() }
        }
    }

    @Test
    fun detail_showsInfoAndResidents_andClickOpensCharacter() = runComposeUiTest {
        var clicked: Int? = null
        setContent {
            LocationDetailContent(
                LocationDetailUiState(isLoading = false, location = citadel, residentCount = 101, residents = listOf(rick)),
                onBack = {}, onRetry = {}, onLoadMoreResidents = {}, onCharacterClick = { clicked = it }
            )
        }

        onNodeWithText("Space station").assertIsDisplayed()
        onNodeWithText("Penghuni (101)").assertIsDisplayed()
        onNodeWithText("Rick Sanchez").performClick()
        assertEquals(1, clicked)
    }

    @Test
    fun detail_withoutResidents_saysSo() = runComposeUiTest {
        setContent {
            LocationDetailContent(
                LocationDetailUiState(isLoading = false, location = earth, residentCount = 0),
                onBack = {}, onRetry = {}, onLoadMoreResidents = {}, onCharacterClick = {}
            )
        }

        onNodeWithText("Tidak ada penghuni yang diketahui.").assertIsDisplayed()
    }

    @Test
    fun detail_loadingResidents_showsFooterIndicator() = runComposeUiTest {
        setContent {
            LocationDetailContent(
                LocationDetailUiState(
                    isLoading = false, location = citadel, residentCount = 101,
                    isLoadingResidents = true, canShowMoreResidents = true
                ),
                onBack = {}, onRetry = {}, onLoadMoreResidents = {}, onCharacterClick = {}
            )
        }

        onNodeWithTag(RESIDENT_LOAD_MORE_INDICATOR_TAG).assertIsDisplayed()
    }

    @Test
    fun detail_notCached_offline_showsErrorAndRetry() = runComposeUiTest {
        var retries = 0
        setContent {
            LocationDetailContent(
                LocationDetailUiState(isLoading = false, errorMessage = "Gagal memuat lokasi. Periksa koneksi internet."),
                onBack = {}, onRetry = { retries++ }, onLoadMoreResidents = {}, onCharacterClick = {}
            )
        }

        onNodeWithText("Gagal memuat lokasi. Periksa koneksi internet.").assertIsDisplayed()
        onNodeWithText("Coba Lagi").performClick()
        assertEquals(1, retries)
    }
}
