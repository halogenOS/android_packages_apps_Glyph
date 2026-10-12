/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.sdk

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import custom.glyph.features.R
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.settings.SwitchRow
import custom.glyph.ui.settings.rememberSetting

object SdkEntry : FeatureEntry {
    override val id = "sdk"

    @Composable
    override fun Row(openPage: () -> Unit) {
        val enabled = rememberSetting(SdkSettings.enabled)
        SwitchRow(
            title = stringResource(R.string.sdk_apps),
            summary = stringResource(R.string.sdk_apps_summary),
            checked = enabled.value,
            onCheckedChange = enabled.set,
        )
    }
}
