/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import custom.glyph.features.R
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeaturePage
import custom.glyph.ui.settings.LinkRow
import custom.glyph.ui.settings.rememberSetting

object TimerEntry : FeatureEntry {
    override val id = "timer"

    @Composable
    override fun Row(openPage: () -> Unit) {
        val seconds = rememberSetting(TimerSettings.durationSeconds)
        LinkRow(stringResource(R.string.timer), stringResource(R.string.timer_summary, timerText(seconds.value * 1000L)), openPage)
    }

    override val page = FeaturePage(R.string.timer) { TimerPage() }

    override val tile = TimerTile::class.java
}
