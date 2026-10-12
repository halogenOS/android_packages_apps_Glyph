/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.flip

import android.media.AudioManager
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import custom.glyph.features.R
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeaturePage
import custom.glyph.ui.settings.ChoiceDialog
import custom.glyph.ui.settings.LinkRow
import custom.glyph.ui.settings.SettingsCard
import custom.glyph.ui.settings.SwitchRow
import custom.glyph.ui.settings.rememberSetting

object FlipEntry : FeatureEntry {
    override val id = "flip"

    private val ringerChoices = listOf(
        AudioManager.RINGER_MODE_VIBRATE to R.string.ringer_vibrate,
        AudioManager.RINGER_MODE_SILENT to R.string.ringer_silent,
        AudioManager.RINGER_MODE_NORMAL to R.string.ringer_normal,
    )

    @Composable
    override fun Row(openPage: () -> Unit) {
        val enabled = rememberSetting(FlipSettings.enabled)
        val ringer = rememberSetting(FlipSettings.ringerMode)
        val summary = if (enabled.value) stringResource(R.string.flip_ringer_summary, stringResource(labelOf(ringer.value))) else stringResource(R.string.off)
        LinkRow(stringResource(R.string.flip), summary, openPage)
    }

    override val page = FeaturePage(R.string.flip) {
        val enabled = rememberSetting(FlipSettings.enabled)
        val ringer = rememberSetting(FlipSettings.ringerMode)
        var choosing by remember { mutableStateOf(false) }

        SettingsCard {
            SwitchRow(stringResource(R.string.flip), enabled.value, enabled.set, summary = stringResource(R.string.flip_summary))
            if (enabled.value) {
                LinkRow(stringResource(R.string.flip_ringer), stringResource(labelOf(ringer.value)), onClick = { choosing = true })
            }
        }
        if (choosing) {
            ChoiceDialog(
                title = stringResource(R.string.flip_ringer),
                choices = ringerChoices.map { it.first },
                selected = ringer.value,
                label = { stringResource(labelOf(it)) },
                onChoose = { ringer.set(it); choosing = false },
                onDismiss = { choosing = false },
            )
        }
    }

    @StringRes
    private fun labelOf(mode: Int): Int = (ringerChoices.firstOrNull { it.first == mode } ?: ringerChoices.first()).second
}
