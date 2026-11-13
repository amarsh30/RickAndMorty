
package com.amr.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Spoken label for full-screen loading, matching the skeleton's single "loading" node. */
const val LOADING_LABEL = "Memuat"

/** Spoken label for a progress bar shown while content already on screen is being updated. */
const val REFRESHING_LABEL = "Memperbarui"

@Composable
fun AppLoading(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(40.dp)
                .semantics { contentDescription = LOADING_LABEL }
        )
    }
}
