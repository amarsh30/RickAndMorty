
package org.amr.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.amr.feature.character.detail.CharacterDetailScreen
import com.amr.feature.character.favorite.FavoritesScreen
import com.amr.feature.character.presentation.CharacterScreen
import com.amr.feature.character.search.CharacterSearchScreen
import kotlinx.serialization.Serializable

@Serializable
internal data object CharacterListRoute

@Serializable
internal data object CharacterSearchRoute

@Serializable
internal data object FavoritesRoute

@Serializable
internal data class CharacterDetailRoute(val characterId: Int)

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        val openDetail: (Int) -> Unit = { id -> navController.navigate(CharacterDetailRoute(id)) }

        NavHost(navController = navController, startDestination = CharacterListRoute) {
            composable<CharacterListRoute> {
                CharacterScreen(
                    onCharacterClick = openDetail,
                    onSearchClick = { navController.navigate(CharacterSearchRoute) },
                    onFavoritesClick = { navController.navigate(FavoritesRoute) }
                )
            }
            composable<CharacterSearchRoute> {
                CharacterSearchScreen(
                    onBack = { navController.popBackStack() },
                    onCharacterClick = openDetail
                )
            }
            composable<FavoritesRoute> {
                FavoritesScreen(
                    onBack = { navController.popBackStack() },
                    onCharacterClick = openDetail
                )
            }
            composable<CharacterDetailRoute> { entry ->
                CharacterDetailScreen(
                    characterId = entry.toRoute<CharacterDetailRoute>().characterId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
