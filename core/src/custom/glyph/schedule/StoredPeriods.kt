/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.schedule

import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Quiet periods as they are stored: day names and minutes of the day, as JSON. */
object StoredPeriods {

    @Serializable
    private class Stored(val id: String, val enabled: Boolean = true, val days: Set<String>, val start: Int, val end: Int)

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(periods: List<QuietPeriod>): String = json.encodeToString(
        periods.map { Stored(it.id, it.enabled, it.days.mapTo(mutableSetOf()) { day -> day.name }, it.start.minuteOfDay, it.end.minuteOfDay) },
    )

    /** The stored periods, or null when none were ever stored or they cannot be read. */
    fun decode(stored: String): List<QuietPeriod>? =
        stored.takeIf { it.isNotBlank() }
            ?.let { runCatching { json.decodeFromString<List<Stored>>(it) }.getOrNull() }
            ?.map { period(it.id, it.enabled, it.days, it.start, it.end) }

    fun period(id: String, enabled: Boolean, days: Set<String>, startMinute: Int, endMinute: Int) = QuietPeriod(
        id = id,
        enabled = enabled,
        days = DayOfWeek.entries.filterTo(mutableSetOf()) { it.name in days },
        start = LocalTime.ofSecondOfDay(startMinute * 60L),
        end = LocalTime.ofSecondOfDay(endMinute * 60L),
    )

    private val LocalTime.minuteOfDay: Int get() = toSecondOfDay() / 60
}
