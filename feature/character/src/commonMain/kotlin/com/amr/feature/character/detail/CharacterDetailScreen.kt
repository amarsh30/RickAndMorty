package com.amr.feature.character.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amr.core.designsystem.component.AppError
import com.amr.core.designsystem.component.AppLoading
import com.amr.core.domain.model.Character
import com.amr.feature.character.component.CharacterImage
import com.amr.feature.character.component.CharacterStatus
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CharacterDetailScreen(
    characterId: Int,
    onBack: () -> Unit,
    onLocationClick: (Int) -> Unit = {},
    viewModel: CharacterDetailViewModel = koinViewModel(
        key = "character-detail-$characterId",
        parameters = { parametersOf(characterId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Connectivity recovery only while this screen is started (visible).
    LifecycleStartEffect(viewModel) {
        viewModel.setVisible(true)
        onStopOrDispose { viewModel.setVisible(false) }
    }

    CharacterDetailContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onToggleFavorite = viewModel::toggleFavorite,
        onLocationClick = onLocationClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CharacterDetailContent(
    state: CharacterDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLocationClick: (Int) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.character?.name ?: "Detail Karakter") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } }
            )
        }
    ) { padding ->
        val character = state.character
        val contentModifier = Modifier.fillMaxSize().padding(padding)

        when {
            state.isLoading -> AppLoading(modifier = contentModifier)

            character == null -> AppError(
                message = state.errorMessage ?: "Data karakter tidak ditemukan.",
                onRetry = onRetry,
                modifier = contentModifier
            )

            else -> Column(
                modifier = contentModifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.isRefreshing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                state.errorMessage?.let { message ->
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }

                CharacterDetailBody(
                    character = character,
                    isFavorite = state.isFavorite,
                    onToggleFavorite = onToggleFavorite,
                    onLocationClick = onLocationClick
                )
            }
        }
    }
}

@Composable
private fun CharacterDetailBody(
    character: Character,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onLocationClick: (Int) -> Unit
) {
    // Caps the width so the layout stays readable on tablets.
    Column(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CharacterImage(
            imageUrl = character.imageUrl,
            contentDescription = "Foto ${character.name}",
            modifier = Modifier.size(200.dp)
        )

        Text(
            text = character.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )
        CharacterStatus(status = character.status)

        if (isFavorite) {
            OutlinedButton(onClick = onToggleFavorite) { Text("Hapus dari Favorit") }
        } else {
            Button(onClick = onToggleFavorite) { Text("Tambah ke Favorit") }
        }

        DetailRow("Spesies", character.species)
        DetailRow("Tipe", character.type?.ifBlank { "-" })
        DetailRow("Gender", character.gender)
        LocationRow("Asal", character.originName, character.originId, onLocationClick)
        LocationRow("Lokasi terakhir", character.locationName, character.locationId, onLocationClick)
        DetailRow("Jumlah episode", character.episodeIds?.size?.toString())

        if (!character.hasDetails) {
            Text(
                text = "Sebagian data belum tersedia offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A location value that opens Location Detail when its id is known. */
@Composable
private fun LocationRow(label: String, name: String?, locationId: Int?, onLocationClick: (Int) -> Unit) {
    if (locationId == null) {
        DetailRow(label, name)
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { onLocationClick(locationId) }) {
            Text(text = name ?: "-")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
