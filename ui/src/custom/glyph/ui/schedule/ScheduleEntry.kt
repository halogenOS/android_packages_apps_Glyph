/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.schedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import custom.glyph.glyph
import custom.glyph.schedule.Schedule
import custom.glyph.ui.R
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeaturePage
import custom.glyph.ui.settings.LinkRow

object ScheduleEntry : FeatureEntry {
    override val id = "schedule"

    @Composable
    override fun Row(openPage: () -> Unit) {
        val glyph = LocalContext.current.glyph
        val schedule by glyph.schedule.collectAsState(glyph.core.defaultSchedule)
        LinkRow(stringResource(R.string.schedule), summary(schedule), openPage)
    }

    override val page = FeaturePage(R.string.schedule) { SchedulePage() }

    /** "Off", the one period in use, or how many are in use. */
    @Composable
    private fun summary(schedule: Schedule): String {
        val inUse = schedule.periods.filter { it.enabled }.takeIf { schedule.enabled }.orEmpty()
        return when (inUse.size) {
            0 -> stringResource(R.string.schedule_off)
            1 -> periodSummary(inUse.single())
            else -> pluralStringResource(R.plurals.schedule_count, inUse.size, inUse.size)
        }
    }
}
