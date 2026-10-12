/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import custom.glyph.ui.R
import custom.glyph.ui.settings.SettingsCard
import custom.glyph.ui.settings.SwitchRow

@Composable
fun BrightnessSettings(
    brightness: Float,
    auto: Boolean,
    autoAvailable: Boolean,
    onBrightness: (Float) -> Unit,
    onAuto: (Boolean) -> Unit,
) {
    val automatic = auto && autoAvailable
    SettingsCard {
        Text(
            stringResource(R.string.brightness),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Slider(
            value = brightness,
            onValueChange = onBrightness,
            valueRange = MIN_BRIGHTNESS..1f,
            enabled = !automatic,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (autoAvailable) {
            SwitchRow(
                title = stringResource(R.string.auto_brightness),
                summary = stringResource(R.string.auto_brightness_summary),
                checked = auto,
                onCheckedChange = onAuto,
            )
        }
    }
}

private const val MIN_BRIGHTNESS = 0.05f
