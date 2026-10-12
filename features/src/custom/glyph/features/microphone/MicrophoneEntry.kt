/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.microphone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import custom.glyph.features.R
import custom.glyph.glyph
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeaturePage
import custom.glyph.ui.settings.LinkRow
import custom.glyph.ui.settings.rememberSetting

object MicrophoneEntry : FeatureEntry {
    override val id = "microphone"

    @Composable
    override fun Row(openPage: () -> Unit) {
        val device = LocalContext.current.glyph.device
        if (!remember(device) { MicrophoneLightFeature.isSupported(device) }) return
        val enabled = rememberSetting(MicrophoneSettings.enabled)
        val apps = rememberSetting(MicrophoneSettings.apps)
        val summary = when {
            !enabled.value -> stringResource(R.string.off)
            apps.value.isEmpty() -> stringResource(R.string.microphone_light_no_apps)
            else -> pluralStringResource(R.plurals.microphone_light_apps, apps.value.size, apps.value.size)
        }
        LinkRow(stringResource(R.string.microphone_light), summary, openPage)
    }

    override val page = FeaturePage(R.string.microphone_light) { MicrophonePage() }
}
