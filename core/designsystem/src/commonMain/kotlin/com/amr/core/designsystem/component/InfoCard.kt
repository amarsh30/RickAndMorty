package com.amr.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.amr.core.designsystem.theme.AppThemeTokens

/** One label/value line of an [InfoCard]; [monospace] suits codes such as "S01E01". */
data class InfoCardRow(val label: String, val value: String?, val monospace: Boolean = false)

/**
 * Outlined card of label/value rows for detail screens. Values wrap instead of
 * being clipped at large font scales; a blank value shows "-".
 */
@Composable
fun InfoCard(rows: List<InfoCardRow>, modifier: Modifier = Modifier) {
    val spacing = AppThemeTokens.spacing
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            rows.forEach { row ->
                val valueStyle: TextStyle =
                    if (row.monospace) AppThemeTokens.mono else MaterialTheme.typography.bodyMedium
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = row.value?.ifBlank { null } ?: "-",
                        style = valueStyle,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
