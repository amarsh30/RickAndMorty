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
import kotlin.test.Test
import kotlin.test.assertEquals

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

    // --- detail ---

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
