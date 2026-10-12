/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.progress

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import custom.glyph.features.R
import custom.glyph.ui.apps.AppSwitchRow
import custom.glyph.ui.apps.installedApps
import custom.glyph.ui.settings.SettingsCard
import custom.glyph.ui.settings.SwitchRow
import custom.glyph.ui.settings.rememberSetting

@Composable
fun ProgressPage() {
    val enabled = rememberSetting(ProgressSettings.enabled)
    val songPosition = rememberSetting(ProgressSettings.songPosition)
    val excluded = rememberSetting(ProgressSettings.excludedApps)
    val apps by installedApps()

    fun showFrom(packageName: String, shown: Boolean) =
        excluded.set(if (shown) excluded.value - packageName else excluded.value + packageName)

    SettingsCard {
        SwitchRow(
            title = stringResource(R.string.progress_enabled),
            summary = stringResource(R.string.progress_enabled_summary),
            checked = enabled.value,
            onCheckedChange = enabled.set,
        )
        SwitchRow(
            title = stringResource(R.string.progress_song_position),
            summary = stringResource(R.string.progress_song_position_summary),
            checked = songPosition.value,
            onCheckedChange = songPosition.set,
            enabled = enabled.value,
        )
    }
    Text(
        text = stringResource(R.string.apps),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
    SettingsCard {
        apps.forEach { app ->
            val shown = app.packageName !in excluded.value
            AppSwitchRow(
                app = app,
                summary = null,
                checked = shown,
                onCheckedChange = { showFrom(app.packageName, it) },
                onClick = { showFrom(app.packageName, !shown) },
            )
        }
    }
}
