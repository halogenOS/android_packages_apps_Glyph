/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import android.os.SystemClock

/** Where a timer stands. Points in time are on the elapsed realtime clock, lengths in milliseconds. */
sealed interface TimerState {

    data object Idle : TimerState

    /** Set to [total], waiting for the phone to be laid face down. */
    data class Ready(val total: Long) : TimerState {
        fun started(now: Long = SystemClock.elapsedRealtime()) = Running(now + total, total)
    }

    data class Running(val endsAt: Long, val total: Long) : TimerState {
        fun remaining(now: Long = SystemClock.elapsedRealtime()): Long = (endsAt - now).coerceAtLeast(0)

        fun paused(now: Long = SystemClock.elapsedRealtime()) = Paused(remaining(now), total)
    }

    data class Paused(val remaining: Long, val total: Long) : TimerState {
        fun resumed(now: Long = SystemClock.elapsedRealtime()) = Running(now + remaining, total)
    }

    /** The time is up. */
    data object Ringing : TimerState
}

/** How much of the timer is left, from 1 when it has not started to 0 when it is over. */
fun TimerState.remainingFraction(now: Long = SystemClock.elapsedRealtime()): Float = when (this) {
    is TimerState.Ready -> 1f
    is TimerState.Running -> remaining(now).toFloat() / total
    is TimerState.Paused -> remaining.toFloat() / total
    TimerState.Idle, TimerState.Ringing -> 0f
}
