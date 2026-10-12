/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.settings

import custom.glyph.schedule.Schedule
import custom.glyph.schedule.StoredPeriods
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Settings that apply to every Glyph, whatever lights it. */
class CoreSettings(defaultBrightness: Float) {
    val enabled = Setting.boolean("enabled", true)
    val brightness = Setting.float("brightness", defaultBrightness)
    val autoBrightness = Setting.boolean("auto_brightness", false)

    val scheduleEnabled = Setting.boolean("schedule_enabled", false)
    val schedulePeriods = Setting.string("schedule_periods", "")

    /** The one period there was before there could be several; it stands until periods are stored. */
    private val firstPeriodDays = Setting.stringSet("schedule_days", DayOfWeek.entries.map { it.name }.toSet())
    private val firstPeriodStart = Setting.int("schedule_start", 22 * 60)
    private val firstPeriodEnd = Setting.int("schedule_end", 7 * 60)

    val defaultSchedule: Schedule
        get() = Schedule(scheduleEnabled.default, listOf(firstPeriod(firstPeriodDays.default, firstPeriodStart.default, firstPeriodEnd.default)))

    fun schedule(settings: GlyphSettings): Flow<Schedule> = combine(
        settings[scheduleEnabled],
        settings[schedulePeriods],
        settings[firstPeriodDays],
        settings[firstPeriodStart],
        settings[firstPeriodEnd],
    ) { enabled, stored, days, start, end ->
        Schedule(enabled, StoredPeriods.decode(stored) ?: listOf(firstPeriod(days, start, end)))
    }

    private fun firstPeriod(days: Set<String>, start: Int, end: Int) = StoredPeriods.period(FIRST_PERIOD, true, days, start, end)

    private companion object {
        const val FIRST_PERIOD = "first"
    }
}
