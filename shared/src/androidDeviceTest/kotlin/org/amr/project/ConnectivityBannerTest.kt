package org.amr.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.amr.core.domain.model.AppConnectivity
import kotlin.test.Test
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class ConnectivityBannerTest {

    @Test
    fun offline_showsNetworkMessage_withoutClaimingSavedData() = runComposeUiTest {
        setContent { ConnectivityBanner(AppConnectivity.Offline) }

        onNodeWithTag(CONNECTIVITY_BANNER_TAG)
            .assertIsDisplayed()
            .assertTextEquals("Tidak ada koneksi internet.")
    }

    @Test
    fun connectedWithoutInternet_andApiDegraded_haveDistinctMessages() = runComposeUiTest {
        var status by mutableStateOf(AppConnectivity.NoInternet)
        setContent { ConnectivityBanner(status) }

        onNodeWithTag(CONNECTIVITY_BANNER_TAG)
            .assertTextEquals("Terhubung ke jaringan, tetapi tanpa akses internet.")

        status = AppConnectivity.ApiDegraded
        onNodeWithTag(CONNECTIVITY_BANNER_TAG)
            .assertTextEquals("Server Rick and Morty sedang bermasalah.")
    }

    @Test
    fun timeout_showsNeutralMessage_thenHidesAfterSuccess() = runComposeUiTest {
        var status by mutableStateOf(AppConnectivity.Unstable)
        setContent { ConnectivityBanner(status) }

        // Not "server bermasalah": a timeout does not prove the server is at fault.
        onNodeWithTag(CONNECTIVITY_BANNER_TAG)
            .assertTextEquals("Koneksi atau layanan sedang mengalami gangguan.")

        status = AppConnectivity.Normal
        waitForIdle()
        onNodeWithTag(CONNECTIVITY_BANNER_TAG).assertDoesNotExist()
    }

    @Test
    fun normal_hidesBanner() = runComposeUiTest {
        var status by mutableStateOf(AppConnectivity.Offline)
        setContent { ConnectivityBanner(status) }
        onNodeWithTag(CONNECTIVITY_BANNER_TAG).assertIsDisplayed()

        status = AppConnectivity.Normal
        waitForIdle()

        onNodeWithTag(CONNECTIVITY_BANNER_TAG).assertDoesNotExist()
        assertNull(AppConnectivity.Normal.bannerMessage())
    }
}
