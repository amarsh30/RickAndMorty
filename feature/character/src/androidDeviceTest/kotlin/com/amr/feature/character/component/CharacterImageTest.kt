package com.amr.feature.character.component

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.component.AVATAR_FALLBACK_TAG
import com.amr.core.designsystem.theme.AppTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CharacterImageTest {

    @Test
    fun missingUrl_showsFallback() = runComposeUiTest {
        setContent { AppTheme { CharacterImage(imageUrl = null, contentDescription = null, modifier = Modifier.size(80.dp)) } }

        onNodeWithTag(AVATAR_FALLBACK_TAG).assertIsDisplayed()
    }

    @Test
    fun unloadableUrl_fallsBackAfterError() = runComposeUiTest {
        // No fetcher handles this scheme, so Coil fails locally without any network request.
        setContent {
            AppTheme { CharacterImage(imageUrl = "invalid-scheme://image", contentDescription = null, modifier = Modifier.size(80.dp)) }
        }

        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasTestTag(AVATAR_FALLBACK_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag(AVATAR_FALLBACK_TAG).assertIsDisplayed()
    }
}
