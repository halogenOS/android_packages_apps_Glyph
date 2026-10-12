/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import custom.glyph.animation.meterFrame
import custom.glyph.frame.Frame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

private const val BLINK_MS = 500L

/**
 * The timer on the ring [leds], ordered from the LED that goes out last. Ready, the full ring
 * blinks; running, it shrinks as the time goes by, the LED at its edge partly lit; paused,
 * what is left blinks. Every change of [shown] shows at once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun timerFrames(frameSize: Int, leds: List<Int>, shown: Flow<TimerState>): Flow<Frame> {
    fun ring(fraction: Float) = meterFrame(frameSize, leds, fraction * leds.size)

    return shown.flatMapLatest { state ->
        when (state) {
            is TimerState.Ready -> blinking(ring(1f))
            is TimerState.Paused -> blinking(ring(state.remainingFraction()))
            is TimerState.Running -> flow {
                val tick = tickFor(state.total)
                do {
                    val left = state.remainingFraction()
                    emit(ring(left))
                    delay(tick)
                } while (left > 0f)
            }
            TimerState.Idle, TimerState.Ringing -> emptyFlow()
        }
    }
}

private fun blinking(frame: Frame): Flow<Frame> = flow {
    val dark = Frame.dark(frame.size)
    while (true) {
        emit(frame)
        delay(BLINK_MS)
        emit(dark)
        delay(BLINK_MS)
    }
}

/** How often the ring moves on: often enough for a short timer, rarely enough to spare a long one. */
private fun tickFor(totalMillis: Long): Long = when {
    totalMillis <= 30_000 -> 50
    totalMillis <= 180_000 -> 100
    else -> 1_000
}
