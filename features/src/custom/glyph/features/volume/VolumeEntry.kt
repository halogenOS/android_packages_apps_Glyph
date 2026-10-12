/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.volume

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import custom.glyph.features.R
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.settings.SwitchRow
import custom.glyph.ui.settings.rememberSetting

object VolumeEntry : FeatureEntry {
    override val id = "volume"

    @Composable
    override fun Row(openPage: () -> Unit) {
        val enabled = rememberSetting(VolumeSettings.enabled)
        SwitchRow(
            title = stringResource(R.string.volume_level),
            summary = stringResource(R.string.volume_level_summary),
            checked = enabled.value,
            onCheckedChange = enabled.set,
        )
    }
}
