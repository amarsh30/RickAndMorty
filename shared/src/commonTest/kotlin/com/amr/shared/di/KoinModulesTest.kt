package com.amr.shared.di

import com.amr.core.data.local.datasource.CharacterLocalDataSource
import com.amr.core.data.local.datasource.CharacterLookupLocalDataSource
import com.amr.core.data.local.model.CharacterPagingMetadata
import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.data.remote.dto.CharacterResponseDto
import com.amr.core.data.remote.dto.PageInfoDto
import com.amr.core.domain.model.Character
import com.amr.core.domain.repository.CharacterRepository
import com.amr.core.domain.usecase.ObserveCharactersUseCase
import com.amr.core.domain.usecase.RefreshCharactersUseCase
import com.amr.feature.character.detail.CharacterDetailViewModel
import com.amr.feature.character.favorite.FavoritesViewModel
import com.amr.feature.character.presentation.CharacterViewModel
import com.amr.feature.character.search.CharacterSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Memastikan graph Koin yang dipakai aplikasi (commonModules) bisa me-resolve
 * semua dependency hingga ViewModel. Sumber data platform (Ktor & Room)
 * di-override dengan fake, sehingga test tidak butuh jaringan / database.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KoinModulesTest {

    private val fakeOverrides: Module = module {
        single<CharacterRemoteDataSource> {
            object : CharacterRemoteDataSource {
                private val empty = CharacterResponseDto(
                    info = PageInfoDto(count = 0, pages = 0, next = null, prev = null),
                    results = emptyList()
                )
                override suspend fun getCharacters(page: Int) = empty
                override suspend fun searchCharacters(
                    name: String?,
                    status: String?,
                    species: String?,
                    gender: String?
                ) = empty
                override suspend fun getCharacter(id: Int) = error("not used")
            }
        }
        single<CharacterLocalDataSource> {
            object : CharacterLocalDataSource {
                private val data = MutableStateFlow<List<Character>>(emptyList())
                private val metadata = MutableStateFlow<CharacterPagingMetadata?>(null)
                override fun observeCharacters(): Flow<List<Character>> = data
                override fun observePagingMetadata(): Flow<CharacterPagingMetadata?> = metadata
                override suspend fun getPagingMetadata(): CharacterPagingMetadata? = metadata.value
                override suspend fun replaceFirstPage(
                    characters: List<Character>,
                    metadata: CharacterPagingMetadata
                ) {
                    data.value = characters
                    this.metadata.value = metadata
                }
                override suspend fun appendPage(
                    characters: List<Character>,
                    metadata: CharacterPagingMetadata
                ) {
                    data.value = data.value + characters
                    this.metadata.value = metadata
                }
            }
        }
        single<CharacterLookupLocalDataSource> {
            object : CharacterLookupLocalDataSource {
                override fun observeSearch(
                    name: String?,
                    status: String?,
                    species: String?,
                    gender: String?
                ): Flow<List<Character>> = flowOf(emptyList())
                override fun observeCharacter(id: Int): Flow<Character?> = flowOf(null)
                override suspend fun saveFetchedCharacters(characters: List<Character>) = Unit
                override fun observeFavorites(): Flow<List<Character>> = flowOf(emptyList())
                override fun observeIsFavorite(id: Int): Flow<Boolean> = flowOf(false)
                override suspend fun addFavorite(character: Character) = Unit
                override suspend fun removeFavorite(id: Int) = Unit
            }
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun commonModules_resolveWholeGraph() {
        val koin = startKoin {
            allowOverride(true)
            modules(commonModules + fakeOverrides)
        }.koin

        assertNotNull(koin.get<CharacterRepository>())
        assertNotNull(koin.get<ObserveCharactersUseCase>())
        assertNotNull(koin.get<RefreshCharactersUseCase>())
        assertNotNull(koin.get<CharacterViewModel>())
        assertNotNull(koin.get<CharacterSearchViewModel>())
        assertNotNull(koin.get<FavoritesViewModel>())
        assertNotNull(koin.get<CharacterDetailViewModel> { parametersOf(1) })
    }

    @Test
    fun repository_isSingleton() {
        val koin = startKoin {
            allowOverride(true)
            modules(commonModules + fakeOverrides)
        }.koin

        assertSame(koin.get<CharacterRepository>(), koin.get<CharacterRepository>())
    }
}
