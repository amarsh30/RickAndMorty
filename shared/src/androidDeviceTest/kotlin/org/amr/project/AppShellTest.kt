package org.amr.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.amr.core.designsystem.theme.AppTheme
import com.amr.core.domain.model.AppConnectivity
import kotlin.test.Test

/**
 * App shell with the real routes and [switchTab], but placeholder screens, so
 * navigation behavior is tested without Koin, ViewModels or network.
 */
@OptIn(ExperimentalTestApi::class)
class AppShellTest {

    private var width by mutableStateOf(400.dp)
    private var navController: NavHostController? = null

    @Composable
    private fun TestApp(connectivity: AppConnectivity = AppConnectivity.Normal) {
        val controller = rememberNavController().also { navController = it }
        var selected by rememberSaveable { mutableStateOf(TopLevelTab.CHARACTERS) }
        // requiredSize simulates a window wider than the test device; anchored at the start edge.
        Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(width, 800.dp)) {
            AppTheme {
                AppShell(
                    selectedTab = selected,
                    onTabSelected = { selected = it; controller.switchTab(it) },
                    connectivity = connectivity
                ) { contentModifier ->
                    TestNavHost(controller, contentModifier)
                }
            }
        }
    }

    @Composable
    private fun TestNavHost(controller: NavHostController, modifier: Modifier) {
        NavHost(controller, startDestination = CharactersGraph, modifier = modifier) {
            navigation<CharactersGraph>(startDestination = CharacterListRoute) {
                composable<CharacterListRoute> {
                    Column {
                        Button(onClick = { controller.navigate(CharacterSearchRoute) }) { Text("Buka Cari") }
                        LazyColumn(state = rememberLazyListState(), modifier = Modifier.testTag("char_list")) {
                            items(100) { Text("Item $it") }
                        }
                    }
                }
                composable<CharacterSearchRoute> {
                    var taps by rememberSaveable { mutableStateOf(0) }
                    Column {
                        Text("Layar Cari")
                        Button(onClick = { taps++ }) { Text("Ketuk $taps") }
                    }
                }
            }
            navigation<LocationsGraph>(startDestination = LocationListRoute) {
                composable<LocationListRoute> { Text("Layar Lokasi") }
            }
            navigation<EpisodesGraph>(startDestination = EpisodeListRoute) {
                composable<EpisodeListRoute> { Text("Layar Episode") }
            }
        }
    }

    @Test
    fun compact_showsBottomBarWithThreeLabelledTabs() = runComposeUiTest {
        width = 400.dp
        setContent { TestApp() }

        onNodeWithTag(NAVIGATION_BAR_TAG).assertIsDisplayed()
        onNodeWithTag(NAVIGATION_RAIL_TAG).assertDoesNotExist()
        listOf("Karakter", "Lokasi", "Episode").forEach { label ->
            // Exactly one node per label: the icon adds no duplicate description.
            onAllNodesWithText(label).assertCountEquals(1)
        }
        TopLevelTab.entries.forEach { onNodeWithTag(it.testTag()).assertHeightIsAtLeast(48.dp) }
        onNodeWithTag(TopLevelTab.CHARACTERS.testTag()).assertIsSelected()
        onNodeWithTag(TopLevelTab.LOCATIONS.testTag()).assertIsNotSelected()
    }

    @Test
    fun mediumAndExpanded_showRailWithThreeTabs() = runComposeUiTest {
        width = 700.dp
        setContent { TestApp() }

        onNodeWithTag(NAVIGATION_RAIL_TAG).assertIsDisplayed()
        onNodeWithTag(NAVIGATION_BAR_TAG).assertDoesNotExist()
        TopLevelTab.entries.forEach { tab ->
            onNode(hasText(tab.label) and hasAnyAncestor(hasTestTag(NAVIGATION_RAIL_TAG))).assertIsDisplayed()
        }

        width = 1000.dp
        waitForIdle()
        onNodeWithTag(NAVIGATION_RAIL_TAG).assertIsDisplayed()
    }

    @Test
    fun switchingTabs_updatesSelectionAndContent() = runComposeUiTest {
        setContent { TestApp() }

        onNodeWithTag(TopLevelTab.LOCATIONS.testTag()).performClick()
        onNodeWithText("Layar Lokasi").assertIsDisplayed()
        onNodeWithTag(TopLevelTab.LOCATIONS.testTag()).assertIsSelected()
        onNodeWithTag(TopLevelTab.CHARACTERS.testTag()).assertIsNotSelected()

        onNodeWithTag(TopLevelTab.EPISODES.testTag()).performClick()
        onNodeWithText("Layar Episode").assertIsDisplayed()
    }

    @Test
    fun tabBackStack_isRestored_andBackReturnsToList() = runComposeUiTest {
        setContent { TestApp() }

        onNodeWithText("Buka Cari").performClick()
        onNodeWithText("Ketuk 0").performClick()
        onNodeWithText("Ketuk 1").assertIsDisplayed()

        onNodeWithTag(TopLevelTab.LOCATIONS.testTag()).performClick()
        onNodeWithText("Layar Lokasi").assertIsDisplayed()
        onNodeWithTag(TopLevelTab.CHARACTERS.testTag()).performClick()

        // Same sub-screen, same saved state.
        onNodeWithText("Ketuk 1").assertIsDisplayed()

        runOnIdle { navController!!.popBackStack() }
        onNodeWithText("Buka Cari").assertIsDisplayed()
    }

    @Test
    fun tabScrollPosition_isRestored() = runComposeUiTest {
        setContent { TestApp() }

        onNodeWithTag("char_list").performScrollToIndex(60)
        onNodeWithText("Item 60").assertIsDisplayed()

        onNodeWithTag(TopLevelTab.EPISODES.testTag()).performClick()
        onNodeWithText("Layar Episode").assertIsDisplayed()
        onNodeWithTag(TopLevelTab.CHARACTERS.testTag()).performClick()

        onNodeWithText("Item 60").assertIsDisplayed()
        onNodeWithText("Item 0").assertDoesNotExist()
    }

    @Test
    fun resizingWindow_swapsNavigationChrome_withoutResettingNavigation() = runComposeUiTest {
        width = 400.dp
        setContent { TestApp() }

        onNodeWithText("Buka Cari").performClick()
        onNodeWithText("Ketuk 0").performClick()
        onNodeWithText("Ketuk 1").performClick()

        width = 900.dp
        waitForIdle()

        onNodeWithTag(NAVIGATION_RAIL_TAG).assertIsDisplayed()
        onNodeWithText("Ketuk 2").assertIsDisplayed()

        width = 400.dp
        waitForIdle()
        onNodeWithTag(NAVIGATION_BAR_TAG).assertIsDisplayed()
        onNodeWithText("Ketuk 2").assertIsDisplayed()
    }

    @Test
    fun connectivityBanner_staysVisibleAboveContentOnEveryTab() = runComposeUiTest {
        setContent { TestApp(connectivity = AppConnectivity.Offline) }

        onNodeWithTag(CONNECTIVITY_BANNER_TAG).assertIsDisplayed()
        onNodeWithText("Buka Cari").assertIsDisplayed()
        onNodeWithTag(TopLevelTab.LOCATIONS.testTag()).performClick()
        onNodeWithTag(CONNECTIVITY_BANNER_TAG).assertIsDisplayed()
        onNodeWithText("Layar Lokasi").assertIsDisplayed()
    }
}
