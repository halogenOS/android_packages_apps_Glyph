/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) { content() }
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, summary: String? = null, enabled: Boolean = true) {
    val usable = enabled && LocalControlsEnabled.current
    SettingsRow(title, summary, onClick = { onCheckedChange(!checked) }, enabled = usable) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = usable)
    }
}

@Composable
fun LinkRow(title: String, summary: String?, onClick: () -> Unit) {
    val usable = LocalControlsEnabled.current
    SettingsRow(title, summary, onClick, enabled = usable) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.dimmedUnless(usable))
    }
}

@Composable
private fun SettingsRow(
    title: String,
    summary: String?,
    onClick: () -> Unit,
    enabled: Boolean,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = TRAILING_GAP).dimmedUnless(enabled)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        trailing()
    }
}

/** Space between a row's text and the switch or arrow beside it. */
private val TRAILING_GAP = 16.dp
