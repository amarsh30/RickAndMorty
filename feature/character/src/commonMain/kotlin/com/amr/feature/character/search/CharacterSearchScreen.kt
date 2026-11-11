package com.amr.feature.character.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amr.core.domain.model.CharacterFilter
import com.amr.feature.character.component.CharacterCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CharacterSearchScreen(
    onBack: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    viewModel: CharacterSearchViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Connectivity recovery only while this screen is started (visible).
    LifecycleStartEffect(viewModel) {
        viewModel.setVisible(true)
        onStopOrDispose { viewModel.setVisible(false) }
    }

    CharacterSearchContent(
        state = state,
        onBack = onBack,
        onFilterChange = viewModel::onFilterChange,
        onRetry = viewModel::retry,
        onCharacterClick = onCharacterClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CharacterSearchContent(
    state: CharacterSearchUiState,
    onBack: () -> Unit,
    onFilterChange: (CharacterFilter) -> Unit,
    onRetry: () -> Unit,
    onCharacterClick: (Int) -> Unit
) {
    val filter = state.filter

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cari Karakter") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filter.name,
                onValueChange = { onFilterChange(filter.copy(name = it)) },
                label = { Text("Nama") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = filter.species,
                onValueChange = { onFilterChange(filter.copy(species = it)) },
                label = { Text("Spesies") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ChipRow(
                options = CharacterFilter.Status.entries,
                selected = filter.status,
                label = { it.label },
                onSelect = { onFilterChange(filter.copy(status = it)) }
            )
            ChipRow(
                options = CharacterFilter.Gender.entries,
                selected = filter.gender,
                label = { it.label },
                onSelect = { onFilterChange(filter.copy(gender = it)) }
            )

            if (state.isSearchingRemote) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (state.isOfflineResult) {
                Row {
                    Text(
                        text = "Offline: hasil dari data tersimpan dan mungkin belum lengkap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRetry) { Text("Coba Lagi") }
                }
            }
            state.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            when {
                filter.isEmpty -> Text(
                    text = "Masukkan nama, spesies, status, atau gender.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                state.results.isEmpty() && !state.isSearchingRemote -> Text(
                    text = "Tidak ada karakter yang cocok.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(items = state.results, key = { it.id }) { character ->
                        CharacterCard(
                            name = character.name,
                            status = character.status,
                            imageUrl = character.imageUrl,
                            onClick = { onCharacterClick(character.id) }
                        )
                    }
                }
            }
        }
    }
}

/** Single-choice chips; tapping the selected chip clears the choice. */
@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T?) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(if (option == selected) null else option) },
                label = { Text(label(option)) }
            )
        }
    }
}

private val CharacterFilter.Status.label: String
    get() = when (this) {
        CharacterFilter.Status.ALIVE -> "Alive"
        CharacterFilter.Status.DEAD -> "Dead"
        CharacterFilter.Status.UNKNOWN -> "Status unknown"
    }

private val CharacterFilter.Gender.label: String
    get() = when (this) {
        CharacterFilter.Gender.FEMALE -> "Female"
        CharacterFilter.Gender.MALE -> "Male"
        CharacterFilter.Gender.GENDERLESS -> "Genderless"
        CharacterFilter.Gender.UNKNOWN -> "Gender unknown"
    }
