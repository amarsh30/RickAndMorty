package org.amr.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.amr.core.domain.model.AppConnectivity
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * The banner text is merged into an always-present live region, so the text node itself
 * is looked up in the unmerged tree.
 */
@OptIn(ExperimentalTestApi::class)
class ConnectivityBannerTest {

    private val politeLiveRegion =
        SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun offline_showsNetworkMessage_withoutClaimingSavedData() = runComposeUiTest {
        setContent { ConnectivityBanner(AppConnectivity.Offline) }

        onNodeWithTag(CONNECTIVITY_BANNER_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("Tidak ada koneksi internet.")
    }

    @Test
    fun connectedWithoutInternet_andApiDegraded_haveDistinctMessages() = runComposeUiTest {
        var status by mutableStateOf(AppConnectivity.NoInternet)
        setContent { ConnectivityBanner(status) }

        onNodeWithTag(CONNECTIVITY_BANNER_TAG, useUnmergedTree = true)
            .assertTextEquals("Terhubung ke jaringan, tetapi tanpa akses internet.")

        status = AppConnectivity.ApiDegraded
        onNodeWithTag(CONNECTIVITY_BANNER_TAG, useUnmergedTree = true)
            .assertTextEquals("Server Rick and Morty sedang bermasalah.")
    }

    @Test
    fun timeout_showsNeutralMessage_thenHidesAfterSuccess() = runComposeUiTest {
        var status by mutableStateOf(AppConnectivity.Unstable)
        setContent { ConnectivityBanner(status) }

        // Not "server bermasalah": a timeout does not prove the server is at fault.
        onNodeWithTag(CONNECTIVITY_BANNER_TAG, useUnmergedTree = true)
            .assertTextEquals("Koneksi atau layanan sedang mengalami gangguan.")

        status = AppConnectivity.Normal
        waitForIdle()
        onNodeWithTag(CONNECTIVITY_BANNER_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun normal_hidesBanner() = runComposeUiTest {
        var status by mutableStateOf(AppConnectivity.Offline)
        setContent { ConnectivityBanner(status) }
        onNodeWithTag(CONNECTIVITY_BANNER_TAG, useUnmergedTree = true).assertIsDisplayed()

        status = AppConnectivity.Normal
        waitForIdle()

        onNodeWithTag(CONNECTIVITY_BANNER_TAG, useUnmergedTree = true).assertDoesNotExist()
        assertNull(AppConnectivity.Normal.bannerMessage())
    }

    // --- Announcement semantics (6C.2B) ---

    @Test
    fun liveRegion_isOnTheAlwaysPresentRegion_withTheMessageMergedIn_andNotDuplicated() = runComposeUiTest {
        var status by mutableStateOf(AppConnectivity.Normal)
        setContent { ConnectivityBanner(status) }

        // Present before any message, so the message appearing is a change of an existing node.
        onNodeWithTag(CONNECTIVITY_BANNER_REGION_TAG).assertExists().assert(politeLiveRegion)
        onNodeWithTag(CONNECTIVITY_BANNER_REGION_TAG).assert(hasText("Tidak ada koneksi internet.").not())

        status = AppConnectivity.Offline
        onNodeWithTag(CONNECTIVITY_BANNER_REGION_TAG)
            .assert(politeLiveRegion)
            .assert(hasText("Tidak ada koneksi internet."))
        // One announcing node: the text itself is not a second live region.
        onNodeWithTag(CONNECTIVITY_BANNER_TAG, useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
        onAllNodesWithText("Tidak ada koneksi internet.").assertCountEquals(1)

        status = AppConnectivity.ApiDegraded
        onNodeWithTag(CONNECTIVITY_BANNER_REGION_TAG).assert(hasText("Server Rick and Morty sedang bermasalah."))
    }
}
