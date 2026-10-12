/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.calls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import custom.glyph.features.R
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeaturePage
import custom.glyph.ui.preview.AnimationPicker
import custom.glyph.ui.preview.animationsIn
import custom.glyph.ui.settings.LinkRow
import custom.glyph.ui.settings.SettingsCard
import custom.glyph.ui.settings.SwitchRow
import custom.glyph.ui.settings.rememberSetting

object CallsEntry : FeatureEntry {
    override val id = "calls"

    @Composable
    override fun Row(openPage: () -> Unit) {
        val enabled = rememberSetting(CallSettings.enabled)
        val animation = rememberSetting(CallSettings.animation)
        val animations by animationsIn(CallSettings.CATEGORY)
        val summary = if (enabled.value) animations.firstOrNull { it.id == animation.value }?.animation?.name else stringResource(R.string.off)
        LinkRow(stringResource(R.string.calls), summary, openPage)
    }

    override val page = FeaturePage(R.string.calls) {
        val enabled = rememberSetting(CallSettings.enabled)
        val animation = rememberSetting(CallSettings.animation)
        val ringtoneLights = rememberSetting(CallSettings.ringtoneLights)
        val animations by animationsIn(CallSettings.CATEGORY)
        SettingsCard {
            SwitchRow(stringResource(R.string.calls_enabled), enabled.value, enabled.set)
            SwitchRow(
                title = stringResource(R.string.ringtone_lights),
                summary = stringResource(R.string.ringtone_lights_summary),
                checked = ringtoneLights.value,
                onCheckedChange = ringtoneLights.set,
                enabled = enabled.value,
            )
        }
        SettingsCard {
            AnimationPicker(animations, animation.value, onSelect = { animation.set(it.id) }, enabled = enabled.value)
        }
    }
}
