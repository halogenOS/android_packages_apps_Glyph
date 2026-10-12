/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.schedule

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/** Quiet periods that keep the Glyphs dark; while [enabled], the Glyphs stay dark whenever one of them is active. */
data class Schedule(val enabled: Boolean, val periods: List<QuietPeriod>) {

    private val inUse: List<QuietPeriod> get() = if (enabled) periods.filter { it.enabled } else emptyList()

    fun isActive(at: LocalDateTime): Boolean = inUse.any { it.isActive(at) }

    /** The next moment after [after] at which [isActive] may change, or null if it never does. */
    fun nextChange(after: LocalDateTime): LocalDateTime? = inUse.mapNotNull { it.nextChange(after) }.minOrNull()
}

/**
 * A quiet period on chosen days. A period that ends before it starts runs overnight and
 * belongs to the day it ends on, so its days are the mornings; one that ends when it starts
 * lasts the whole day.
 */
data class QuietPeriod(
    val id: String,
    val enabled: Boolean,
    val days: Set<DayOfWeek>,
    val start: LocalTime,
    val end: LocalTime,
) {
    fun isActive(at: LocalDateTime): Boolean {
        val time = at.toLocalTime()
        val today = at.dayOfWeek in days
        return when {
            start < end -> today && time >= start && time < end
            isOvernight -> (at.dayOfWeek.plus(1) in days && time >= start) || (today && time < end)
            else -> today
        }
    }

    val isOvernight: Boolean get() = start > end

    /** The next moment after [after] at which [isActive] may change, or null if it never does. */
    fun nextChange(after: LocalDateTime): LocalDateTime? {
        if (days.isEmpty()) return null
        return listOf(start, end, LocalTime.MIDNIGHT)
            .map { boundary -> after.toLocalDate().atTime(boundary).let { if (it > after) it else it.plusDays(1) } }
            .min()
    }
}
