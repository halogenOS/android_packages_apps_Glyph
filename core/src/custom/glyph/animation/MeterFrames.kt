/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.animation

import android.os.SystemClock
import custom.glyph.frame.Frame
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * A level shown on [leds], ordered from empty to full: it fills up to [level] (0 to 1), stays
 * for [holdMillis] and drains again.
 */
fun meterFrames(frameSize: Int, leds: List<Int>, level: Float, holdMillis: Long): Flow<Frame> =
    liveMeterFrames(frameSize, leds, flowOf(level), holdMillis)

/**
 * A level shown on [leds] that follows [levels]. The fill glides towards the newest level and
 * the LED at its edge is partly lit, so every change shows and nothing jumps. Once the fill has
 * rested for [holdMillis] without a new level, it drains and the flow completes.
 */
fun liveMeterFrames(frameSize: Int, leds: List<Int>, levels: Flow<Float>, holdMillis: Long): Flow<Frame> = channelFlow {
    val latest = levels.stateIn(this, SharingStarted.Eagerly, 0f)
    val fill = MeterFill(frameSize, leds)
    var target = 0f
    var lastChange = SystemClock.uptimeMillis()

    suspend fun glideTo(goal: Float) {
        if (!fill.stepTowards(goal)) return
        lastChange = SystemClock.uptimeMillis()
        send(fill.frame())
    }

    while (fill.filled != target || SystemClock.uptimeMillis() - lastChange < holdMillis) {
        val newest = latest.value.coerceIn(0f, 1f) * leds.size
        if (newest != target) {
            target = newest
            lastChange = SystemClock.uptimeMillis()
        }
        glideTo(target)
        delay(METER_TICK_MS)
    }
    coroutineContext.cancelChildren()
    while (fill.filled > 0f) {
        glideTo(0f)
        delay(METER_TICK_MS)
    }
}
