/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.visualizer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import custom.glyph.features.R
import custom.glyph.glyph
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.settings.SwitchRow
import custom.glyph.ui.settings.rememberSetting

object VisualizerEntry : FeatureEntry {
    override val id = "visualizer"

    @Composable
    override fun Row(openPage: () -> Unit) {
        val device = LocalContext.current.glyph.device
        if (!remember(device) { MusicVisualizerFeature.isSupported(device) }) return
        val enabled = rememberSetting(VisualizerSettings.enabled)
        SwitchRow(
            title = stringResource(R.string.music_visualizer),
            summary = stringResource(R.string.music_visualizer_summary),
            checked = enabled.value,
            onCheckedChange = enabled.set,
        )
    }
}
