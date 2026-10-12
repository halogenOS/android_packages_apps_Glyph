/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.progress

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import custom.glyph.features.R
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeaturePage
import custom.glyph.ui.settings.LinkRow
import custom.glyph.ui.settings.rememberSetting

object ProgressEntry : FeatureEntry {
    override val id = "progress"

    @Composable
    override fun Row(openPage: () -> Unit) {
        val enabled = rememberSetting(ProgressSettings.enabled)
        val summary = stringResource(if (enabled.value) R.string.progress_summary else R.string.off)
        LinkRow(stringResource(R.string.progress), summary, openPage)
    }

    override val page = FeaturePage(R.string.progress) { ProgressPage() }
}
