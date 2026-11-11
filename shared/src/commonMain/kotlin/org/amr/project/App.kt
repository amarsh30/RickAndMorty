
package org.amr.project

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.amr.feature.character.detail.CharacterDetailScreen
import com.amr.feature.character.favorite.FavoritesScreen
import com.amr.feature.character.presentation.CharacterScreen
import com.amr.feature.character.search.CharacterSearchScreen
import com.amr.feature.episode.detail.EpisodeDetailScreen
import com.amr.feature.episode.presentation.EpisodeListScreen
import com.amr.feature.location.detail.LocationDetailScreen
import com.amr.feature.location.presentation.LocationListScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// --- Tab graphs ---

@Serializable
internal data object CharactersGraph

@Serializable
internal data object LocationsGraph

@Serializable
internal data object EpisodesGraph

// --- Destinations ---

@Serializable
internal data object CharacterListRoute

@Serializable
internal data object CharacterSearchRoute

@Serializable
internal data object FavoritesRoute

@Serializable
internal data class CharacterDetailRoute(val characterId: Int)

@Serializable
internal data object LocationListRoute

@Serializable
internal data class LocationDetailRoute(val locationId: Int)

@Serializable
internal data object EpisodeListRoute

@Serializable
internal data class EpisodeDetailRoute(val episodeId: Int)

internal enum class TopLevelTab(val label: String, val graph: Any, val graphClass: KClass<*>) {
    CHARACTERS("Karakter", CharactersGraph, CharactersGraph::class),
    LOCATIONS("Lokasi", LocationsGraph, LocationsGraph::class),
    EPISODES("Episode", EpisodesGraph, EpisodesGraph::class)
}

internal fun TopLevelTab.testTag() = "tab_${name.lowercase()}"

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        val currentEntry by navController.currentBackStackEntryAsState()
        var selectedTab by rememberSaveable { mutableStateOf(TopLevelTab.CHARACTERS) }

        // Detail screens are root-level destinations, so they keep the tab they were opened from.
        LaunchedEffect(currentEntry) {
            val hierarchy = currentEntry?.destination?.hierarchy ?: return@LaunchedEffect
            TopLevelTab.entries
                .firstOrNull { tab -> hierarchy.any { it.hasRoute(tab.graphClass) } }
                ?.let { selectedTab = it }
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selectedTab,
                            onClick = {
                                selectedTab = tab
                                navController.switchTab(tab)
                            },
                            icon = {},
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag(tab.testTag())
                        )
                    }
                }
            }
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
            )
        }
    }
}

/** Each tab keeps its own back stack and scroll state across switches. */
private fun NavHostController.switchTab(tab: TopLevelTab) {
    navigate(tab.graph) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier) {
    val openCharacter: (Int) -> Unit = { navController.navigate(CharacterDetailRoute(it)) }
    val openLocation: (Int) -> Unit = { navController.navigate(LocationDetailRoute(it)) }
    val openEpisode: (Int) -> Unit = { navController.navigate(EpisodeDetailRoute(it)) }
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = CharactersGraph, modifier = modifier) {
        navigation<CharactersGraph>(startDestination = CharacterListRoute) {
            composable<CharacterListRoute> {
                CharacterScreen(
                    onCharacterClick = openCharacter,
                    onSearchClick = { navController.navigate(CharacterSearchRoute) },
                    onFavoritesClick = { navController.navigate(FavoritesRoute) }
                )
            }
            composable<CharacterSearchRoute> {
                CharacterSearchScreen(onBack = back, onCharacterClick = openCharacter)
            }
            composable<FavoritesRoute> {
                FavoritesScreen(onBack = back, onCharacterClick = openCharacter)
            }
        }

        navigation<LocationsGraph>(startDestination = LocationListRoute) {
            composable<LocationListRoute> {
                LocationListScreen(onLocationClick = openLocation)
            }
        }

        navigation<EpisodesGraph>(startDestination = EpisodeListRoute) {
            composable<EpisodeListRoute> {
                EpisodeListScreen(onEpisodeClick = openEpisode)
            }
        }

        composable<CharacterDetailRoute> { entry ->
            CharacterDetailScreen(
                characterId = entry.toRoute<CharacterDetailRoute>().characterId,
                onBack = back,
                onLocationClick = openLocation
            )
        }
        composable<LocationDetailRoute> { entry ->
            LocationDetailScreen(
                locationId = entry.toRoute<LocationDetailRoute>().locationId,
                onBack = back,
                onCharacterClick = openCharacter
            )
        }
        composable<EpisodeDetailRoute> { entry ->
            EpisodeDetailScreen(
                episodeId = entry.toRoute<EpisodeDetailRoute>().episodeId,
                onBack = back,
                onCharacterClick = openCharacter
            )
        }
    }
}
