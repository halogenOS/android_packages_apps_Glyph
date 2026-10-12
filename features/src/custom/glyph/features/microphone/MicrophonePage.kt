/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.microphone

import android.Manifest
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import custom.glyph.features.R
import custom.glyph.ui.apps.AppSwitchRow
import custom.glyph.ui.apps.installedApps
import custom.glyph.ui.settings.ChoiceDialog
import custom.glyph.ui.settings.LinkRow
import custom.glyph.ui.settings.SettingsCard
import custom.glyph.ui.settings.SwitchRow
import custom.glyph.ui.settings.rememberSetting

@Composable
fun MicrophonePage() {
    val enabled = rememberSetting(MicrophoneSettings.enabled)
    val modeName = rememberSetting(MicrophoneSettings.mode)
    val chosen = rememberSetting(MicrophoneSettings.apps)
    val apps by installedApps(Manifest.permission.RECORD_AUDIO)
    var choosingMode by remember { mutableStateOf(false) }
    val mode = MicrophoneLightMode.named(modeName.value)

    fun watch(packageName: String, watched: Boolean) =
        chosen.set(if (watched) chosen.value + packageName else chosen.value - packageName)

    SettingsCard {
        SwitchRow(
            title = stringResource(R.string.microphone_light_enabled),
            summary = stringResource(R.string.microphone_light_enabled_summary),
            checked = enabled.value,
            onCheckedChange = enabled.set,
        )
        LinkRow(stringResource(R.string.microphone_light_style), stringResource(mode.label), onClick = { choosingMode = true })
    }
    Text(
        text = stringResource(R.string.apps),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
    SettingsCard {
        apps.forEach { app ->
            val watched = app.packageName in chosen.value
            AppSwitchRow(
                app = app,
                summary = null,
                checked = watched,
                onCheckedChange = { watch(app.packageName, it) },
                onClick = { watch(app.packageName, !watched) },
            )
        }
    }
    if (choosingMode) {
        ChoiceDialog(
            title = stringResource(R.string.microphone_light_style),
            choices = MicrophoneLightMode.entries,
            selected = mode,
            label = { stringResource(it.label) },
            onChoose = { modeName.set(it.name); choosingMode = false },
            onDismiss = { choosingMode = false },
        )
    }
}
