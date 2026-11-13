package com.amr.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import com.amr.core.designsystem.icon.AppIcons
import com.amr.core.designsystem.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class ComponentsTest {

    @Test
    fun statusChip_mapsApiStatus() {
        assertEquals(LifeStatus.Alive, LifeStatus.fromApi("Alive"))
        assertEquals(LifeStatus.Dead, LifeStatus.fromApi("dead"))
        assertEquals(LifeStatus.Unknown, LifeStatus.fromApi("unknown"))
        assertEquals(LifeStatus.Unknown, LifeStatus.fromApi(""))
    }

    @Test
    fun statusChip_showsLabelAsSingleNode() = runComposeUiTest {
        setContent { AppTheme { StatusChip(label = "Dead") } }

        // The chip merges icon + label into one node that reads as the label.
        onNodeWithTag(STATUS_CHIP_TAG).assert(hasText("Dead")).assertIsDisplayed()
    }

    @Test
    fun appMessage_withAction_showsButtonAndInvokesIt() = runComposeUiTest {
        var clicks = 0
        setContent {
            AppTheme {
                AppMessage(
                    icon = AppIcons.Info,
                    title = "Belum ada data",
                    description = "Tarik untuk memuat ulang.",
                    actionText = "Muat",
                    onAction = { clicks++ }
                )
            }
        }

        onNodeWithText("Belum ada data").assertIsDisplayed()
        onNodeWithText("Tarik untuk memuat ulang.").assertIsDisplayed()
        onNodeWithText("Muat").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun appMessage_withoutAction_hasNoButton() = runComposeUiTest {
        setContent { AppTheme { AppMessage(icon = AppIcons.Info, title = "Kosong", actionText = "Muat") } }

        onNodeWithText("Kosong").assertIsDisplayed()
        // actionText without a handler must not produce a dead button.
        onNodeWithText("Muat").assertDoesNotExist()
    }

    @Test
    fun appError_keepsRetryText() = runComposeUiTest {
        var retried = false
        setContent { AppTheme { AppError(message = "Gagal", onRetry = { retried = true }) } }

        onNodeWithText("Gagal").assertIsDisplayed()
        onNodeWithText("Coba Lagi").performClick()
        assertEquals(true, retried)
    }

    @Test
    fun infoBanner_rendersInLightAndDark_withDifferentContainers() = runComposeUiTest {
        val containers = mutableMapOf<Boolean, Color>()
        setContent {
            androidx.compose.foundation.layout.Column {
                listOf(false, true).forEach { dark ->
                    AppTheme(darkTheme = dark) {
                        containers[dark] = MaterialTheme.colorScheme.secondaryContainer
                        InfoBanner(message = if (dark) "Gelap" else "Terang")
                    }
                }
            }
        }

        onNodeWithText("Terang").assertIsDisplayed()
        onNodeWithText("Gelap").assertIsDisplayed()
        assertNotEquals(containers[false], containers[true])
    }

    @Test
    fun skeletonList_exposesOnlyALoadingNode() = runComposeUiTest {
        setContent { AppTheme { SkeletonList(count = 3) } }

        val node = onNodeWithTag(SKELETON_LIST_TAG)
        node.assertIsDisplayed()
        node.assert(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Memuat")))
        // Placeholder rows are cleared from semantics: no children are exposed.
        assertEquals(0, node.fetchSemanticsNode().children.size)
    }

    @Test
    fun appLoading_isOneLabelledIndeterminateProgressNode() = runComposeUiTest {
        setContent { AppTheme { AppLoading() } }

        onNodeWithContentDescription(LOADING_LABEL)
            .assertIsDisplayed()
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
        onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertCountEquals(1)
    }

    @Test
    fun loadMoreFooter_spinnerIsLabelled_errorIsPoliteLiveRegion_retryIsAButton() = runComposeUiTest {
        var loading by mutableStateOf(true)
        var retries = 0
        setContent {
            AppTheme {
                LoadMoreFooter(
                    isLoadingMore = loading,
                    loadMoreErrorMessage = "Timeout",
                    isEndReached = false,
                    endReachedText = null,
                    loadMoreIndicatorTag = "footer",
                    onLoadMore = { retries++ }
                )
            }
        }

        onNodeWithTag("footer")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(LOAD_MORE_LOADING_LABEL)))

        loading = false
        onNodeWithText("Timeout")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        onNodeWithText(LOAD_MORE_RETRY_LABEL).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun loadMoreFooter_endOfList_isPlainTextWithoutLiveRegion() = runComposeUiTest {
        setContent {
            AppTheme {
                LoadMoreFooter(
                    isLoadingMore = false,
                    loadMoreErrorMessage = null,
                    isEndReached = true,
                    endReachedText = "Semua sudah ditampilkan",
                    loadMoreIndicatorTag = "footer",
                    onLoadMore = {}
                )
            }
        }

        onNodeWithText("Semua sudah ditampilkan")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun avatarItem_withoutImage_showsFallbackAndHandlesClick() = runComposeUiTest {
        var clicked = false
        setContent { AppTheme { AvatarItem(title = "Morty Smith", subtitle = "Human", onClick = { clicked = true }) } }

        onNodeWithTag(AVATAR_FALLBACK_TAG, useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Morty Smith").performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun infoBanner_isAPoliteLiveRegion() = runComposeUiTest {
        setContent { AppTheme { InfoBanner(message = "Gagal memperbarui data.") } }

        onNodeWithTag(INFO_BANNER_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun errorMessage_isAnnounced_emptyMessageIsNot() = runComposeUiTest {
        setContent {
            AppTheme {
                androidx.compose.foundation.layout.Column {
                    AppError(message = "Gagal memuat", onRetry = {})
                    AppEmpty(message = "Kosong")
                }
            }
        }

        onNodeWithText("Gagal memuat")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        onNodeWithText("Kosong").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun appBarAction_showsPrimaryFocusRing_onlyWhileFocused() = runComposeUiTest {
        var primary = Color.Unspecified
        lateinit var inputMode: InputModeManager
        setContent {
            inputMode = LocalInputModeManager.current
            AppTheme {
                primary = MaterialTheme.colorScheme.primary
                AppBarAction(icon = AppIcons.Search, label = "Cari", onClick = {})
            }
        }
        val node = onNodeWithContentDescription("Cari")
        fun edgePixel(): Color {
            val map = node.captureToImage().toPixelMap()
            return map[3, map.height / 2]
        }

        assertNotEquals(primary, edgePixel(), "no ring before focus")
        // In touch mode Compose clickables are not focusable; a keyboard user is in keyboard mode.
        runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        node.requestFocus()
        waitForIdle()
        assertEquals(primary, edgePixel(), "ring drawn in primary while focused")
        // Touch target unchanged.
        node.assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun sectionHeader_isHeading() = runComposeUiTest {
        setContent { AppTheme { SectionHeader(title = "Season 1", trailing = "11") } }

        onNodeWithText("Season 1").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        onNodeWithText("11").assertIsDisplayed()
    }
}
