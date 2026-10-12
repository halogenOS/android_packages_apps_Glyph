/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import android.content.Context
import android.content.Intent
import custom.glyph.signal.broadcasts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

private const val ACTION_TIMER_STATE = "com.android.deskclock.action.TIMER_STATE"
private const val EXTRA_STATE = "state"
private const val EXTRA_ELAPSED_MILLIS = "elapsed_millis"
private const val EXTRA_TOTAL_MILLIS = "total_millis"
private const val EXTRA_SENT_AT = "sent_at"

/**
 * The timer of the clock app that matters most, as the clock app reports it on every change
 * and every second while it runs. Its countdown is carried forward from the moment of each
 * report, so it stays exact while the reports pause during sleep.
 */
fun Context.clockTimer(): Flow<TimerState> =
    broadcasts(ACTION_TIMER_STATE, fromApps = true)
        .map { it.toTimerState() }
        .onStart { emit(TimerState.Idle) }
        .distinctUntilChanged()

/** The clock app clears the total length of a timer once it rings, so only a counting one needs it. */
private fun Intent.toTimerState(): TimerState {
    val state = getStringExtra(EXTRA_STATE)
    if (state == "expired") return TimerState.Ringing
    val total = getLongExtra(EXTRA_TOTAL_MILLIS, 0)
    if (total <= 0) return TimerState.Idle
    val remaining = total - getLongExtra(EXTRA_ELAPSED_MILLIS, 0)
    return when (state) {
        "running" -> TimerState.Running(getLongExtra(EXTRA_SENT_AT, 0) + remaining, total)
        "paused" -> TimerState.Paused(remaining, total)
        else -> TimerState.Idle
    }
}
