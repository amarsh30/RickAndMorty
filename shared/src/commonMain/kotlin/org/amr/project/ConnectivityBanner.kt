package org.amr.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.amr.core.domain.model.AppConnectivity

internal const val CONNECTIVITY_BANNER_TAG = "connectivity_banner"
internal const val CONNECTIVITY_BANNER_REGION_TAG = "connectivity_banner_region"

/**
 * Global network/API status. It never claims saved data is available: whether a
 * screen has cached data is shown by that screen's own message.
 */
internal fun AppConnectivity.bannerMessage(): String? = when (this) {
    AppConnectivity.Offline -> "Tidak ada koneksi internet."
    AppConnectivity.NoInternet -> "Terhubung ke jaringan, tetapi tanpa akses internet."
    // Neutral on purpose: a timeout does not prove the server is at fault.
    AppConnectivity.Unstable -> "Koneksi atau layanan sedang mengalami gangguan."
    AppConnectivity.ApiDegraded -> "Server Rick and Morty sedang bermasalah."
    AppConnectivity.Normal -> null
}

@Composable
internal fun ConnectivityBanner(status: AppConnectivity, modifier: Modifier = Modifier) {
    val message = status.bannerMessage()
    // The polite live region sits on a node that always exists, and merges the banner text:
    // the banner appearing or changing is then a content change of that node, which screen
    // readers announce, rather than a new node being inserted by the animation. Recomposing
    // with the same message changes nothing, so it is not announced again. When there is no
    // message the region has no text and is not a screen-reader stop. The caller's modifier
    // (e.g. status-bar insets) stays on the animated content, so the empty region takes no space.
    Box(
        modifier = Modifier
            .testTag(CONNECTIVITY_BANNER_REGION_TAG)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        AnimatedVisibility(visible = message != null, modifier = modifier) {
            Text(
                text = message.orEmpty(),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(CONNECTIVITY_BANNER_TAG)
            )
        }
    }
}
