package org.amr.project

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.amr.core.designsystem.component.focusRing
import com.amr.core.designsystem.icon.AppIcons
import com.amr.core.domain.model.AppConnectivity

/** Width buckets from the Material window size classes, based on the space the app actually gets. */
internal enum class WindowWidthClass { Compact, Medium, Expanded }

internal fun widthClassOf(width: Dp): WindowWidthClass = when {
    width < 600.dp -> WindowWidthClass.Compact
    width < 840.dp -> WindowWidthClass.Medium
    else -> WindowWidthClass.Expanded
}

internal const val NAVIGATION_BAR_TAG = "navigation_bar"
internal const val NAVIGATION_RAIL_TAG = "navigation_rail"

internal val TopLevelTab.icon: ImageVector
    get() = when (this) {
        TopLevelTab.CHARACTERS -> AppIcons.Person
        TopLevelTab.LOCATIONS -> AppIcons.Public
        TopLevelTab.EPISODES -> AppIcons.Movie
    }

/**
 * App frame: global connectivity banner on top, and tab navigation as a bottom
 * bar (compact) or a side rail (medium/expanded). [content] is always called at
 * the same place, so resizing the window swaps only the navigation chrome and
 * keeps the NavHost, its back stacks and ViewModels.
 */
@Composable
internal fun AppShell(
    selectedTab: TopLevelTab,
    onTabSelected: (TopLevelTab) -> Unit,
    connectivity: AppConnectivity,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = widthClassOf(maxWidth) != WindowWidthClass.Compact

        Scaffold(
            topBar = { ConnectivityBanner(connectivity, Modifier.windowInsetsPadding(WindowInsets.statusBars)) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(modifier = Modifier.testTag(NAVIGATION_BAR_TAG)) {
                        TopLevelTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = tab == selectedTab,
                                onClick = { onTabSelected(tab) },
                                // The label names the tab; the icon is decorative.
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag()).focusRing(MaterialTheme.shapes.large)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.testTag(NAVIGATION_RAIL_TAG)) {
                        TopLevelTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = tab == selectedTab,
                                onClick = { onTabSelected(tab) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag()).focusRing(MaterialTheme.shapes.large)
                            )
                        }
                    }
                }
                content(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}
