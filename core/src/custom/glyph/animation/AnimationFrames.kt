/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.animation

import android.os.SystemClock
import custom.glyph.frame.Frame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

private const val FADE_TICK_MS = 16L

/** The animation over and over, until the collector stops. */
fun Animation.looping(): Flow<Frame> = flow {
    while (true) emitAll(frames())
}

/**
 * The animation as frames in real time. Every step is timed against the moment collection
 * started, so lateness never accumulates over long animations; fades are sampled at about
 * 60 frames per second. The flow completes once the last step has run out.
 */
fun Animation.frames(): Flow<Frame> = flow {
    val start = SystemClock.uptimeMillis()
    var previous = steps.firstOrNull()?.levels?.map { 0f } ?: return@flow
    var elapsed = 0L

    suspend fun waitUntil(offset: Long) = delay(start + offset - SystemClock.uptimeMillis())

    for (step in steps) {
        val stepStart = elapsed
        elapsed += step.duration
        if (step.transition == Animation.Transition.FADE && step.duration > 0) {
            var offset = stepStart
            while (offset < elapsed) {
                val progress = (offset - stepStart).toFloat() / step.duration
                emit(Frame.of(previous.zip(step.levels) { from, to -> from + (to - from) * progress }))
                offset += FADE_TICK_MS
                waitUntil(minOf(offset, elapsed))
            }
        } else {
            emit(Frame.of(step.levels))
            waitUntil(elapsed)
        }
        previous = step.levels
    }
}
