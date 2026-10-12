/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import custom.glyph.settings.Setting

object TimerSettings {
    val durationSeconds = Setting.int("timer_duration_seconds", 5 * 60)
    val muteAlarm = Setting.boolean("timer_mute_alarm", false)
    val clockTimers = Setting.boolean("timer_clock_timers", true)
}
