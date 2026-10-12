/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import custom.glyph.Glyph
import custom.glyph.schedule.QuietPeriod
import custom.glyph.schedule.Schedule
import custom.glyph.schedule.StoredPeriods
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

/** The quiet periods that keep the Glyphs dark, and how the user changes them. */
class ScheduleViewModel(private val glyph: Glyph) : ViewModel() {

    val schedule: StateFlow<Schedule> = glyph.schedule.stateIn(viewModelScope, SharingStarted.Eagerly, glyph.core.defaultSchedule)

    fun setEnabled(value: Boolean) {
        viewModelScope.launch { glyph.settings.set(glyph.core.scheduleEnabled, value) }
    }

    /** Adds a period from 22:00 to 07:00 on the days no other period covers yet. */
    fun add() = change { periods ->
        val free = DayOfWeek.entries.toSet() - periods.flatMapTo(mutableSetOf()) { it.days }
        periods + QuietPeriod(UUID.randomUUID().toString(), true, free, NEW_START, NEW_END)
    }

    fun remove(id: String) = change { periods -> periods.filterNot { it.id == id } }

    fun setPeriodEnabled(id: String, enabled: Boolean) = edit(id) { it.copy(enabled = enabled) }

    fun toggleDay(id: String, day: DayOfWeek) = edit(id) { it.copy(days = if (day in it.days) it.days - day else it.days + day) }

    fun setStart(id: String, time: LocalTime) = edit(id) { it.copy(start = time) }

    fun setEnd(id: String, time: LocalTime) = edit(id) { it.copy(end = time) }

    private fun edit(id: String, block: (QuietPeriod) -> QuietPeriod) = change { periods -> periods.map { if (it.id == id) block(it) else it } }

    private fun change(block: (List<QuietPeriod>) -> List<QuietPeriod>) {
        val periods = block(schedule.value.periods)
        viewModelScope.launch { glyph.settings.set(glyph.core.schedulePeriods, StoredPeriods.encode(periods)) }
    }

    private companion object {
        val NEW_START: LocalTime = LocalTime.of(22, 0)
        val NEW_END: LocalTime = LocalTime.of(7, 0)
    }
}
