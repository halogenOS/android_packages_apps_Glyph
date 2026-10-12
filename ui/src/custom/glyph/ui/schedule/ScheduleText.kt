/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.schedule

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import custom.glyph.schedule.QuietPeriod
import custom.glyph.ui.R
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** [time] as the phone's clock shows it, in 24 or 12 hours. */
@Composable
fun clockTime(time: LocalTime): String =
    time.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(LocalContext.current)) "HH:mm" else "h:mm a"))

/** The days of the week in the order the [locale] starts its week with. */
fun weekDays(locale: Locale): List<DayOfWeek> = WeekFields.of(locale).firstDayOfWeek.let { first -> (0L until 7L).map(first::plus) }

/** The hours of [period] and the days it applies to, such as "22:00 (day before)–07:00 · Mon, Tue". */
@Composable
fun periodSummary(period: QuietPeriod): String =
    stringResource(R.string.schedule_when, stringResource(R.string.schedule_hours, startTime(period), clockTime(period.end)), days(period.days))

@Composable
private fun startTime(period: QuietPeriod): String =
    clockTime(period.start).let { if (period.isOvernight) stringResource(R.string.schedule_day_before, it) else it }

@Composable
private fun days(days: Set<DayOfWeek>): String {
    val locale = Locale.getDefault()
    return when (days.size) {
        DayOfWeek.entries.size -> stringResource(R.string.schedule_every_day)
        0 -> stringResource(R.string.schedule_no_days)
        else -> weekDays(locale).filter { it in days }.joinToString { it.getDisplayName(TextStyle.SHORT, locale) }
    }
}
