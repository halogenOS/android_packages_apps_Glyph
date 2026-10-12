/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.progress

import android.os.SystemClock
import custom.glyph.animation.METER_TICK_MS
import custom.glyph.animation.MeterFill
import custom.glyph.frame.Frame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlin.math.ceil

private const val FLASH_MS = 150L
private const val IDLE_AFTER_MS = 5_000L
private const val PULSE_MS = 250L
private const val PULSE_DIM = 0.35f
private const val FINISH_FLICKERS = 3
private const val FINISH_HOLD_MS = 1_000L
private const val FINISH_FADE_MS = 500L

/**
 * The ring on [leds] following [tracked]: it glides to each new value, flashes once a milestone
 * is passed and, after a while without change, pulses its tip between full and dim. Reaching
 * the end flickers and fades out; once nothing is tracked any more it drains. Either way the
 * flow completes.
 */
fun progressFrames(frameSize: Int, leds: List<Int>, tracked: StateFlow<TrackedProgress?>): Flow<Frame> = flow {
    val fill = MeterFill(frameSize, leds)
    val ring = Frame.dark(frameSize).with(leds, 1f)
    var followed: TrackedProgress? = null
    var lastChange = SystemClock.uptimeMillis()
    var milestonePassed = false
    var pulseBright = true
    var lastPulse = 0L

    while (true) {
        val progress = tracked.value ?: return@flow drain(fill)
        if (progress != followed) {
            val before = followed?.takeIf { it.key == progress.key }?.fraction ?: progress.fraction
            milestonePassed = milestonePassed || progress.milestones.any { it > before && it <= progress.fraction }
            followed = progress
            lastChange = SystemClock.uptimeMillis()
        }
        val now = SystemClock.uptimeMillis()
        when {
            fill.stepTowards(progress.fraction * leds.size) -> emit(fill.frame())
            milestonePassed -> {
                emit(ring)
                delay(FLASH_MS)
                emit(fill.frame())
                milestonePassed = false
            }
            progress.completes && progress.fraction >= 1f -> return@flow finish(fill, ring)
            now - lastChange >= IDLE_AFTER_MS && now - lastPulse >= PULSE_MS -> {
                pulseBright = !pulseBright
                lastPulse = now
                emit(fill.frameWithTip(if (pulseBright) 1f else PULSE_DIM))
            }
        }
        delay(METER_TICK_MS)
    }
}

/** The fill with its last lit LED at [level]. */
private fun MeterFill.frameWithTip(level: Float): Frame {
    val tip = ceil(filled).toInt() - 1
    return if (tip in leds.indices) frame().with(listOf(leds[tip]), level) else frame()
}

private suspend fun FlowCollector<Frame>.drain(fill: MeterFill) {
    while (fill.stepTowards(0f)) {
        emit(fill.frame())
        delay(METER_TICK_MS)
    }
}

private suspend fun FlowCollector<Frame>.finish(fill: MeterFill, ring: Frame) {
    val dark = Frame.dark(ring.size)
    repeat(FINISH_FLICKERS) {
        emit(dark)
        delay(FLASH_MS)
        emit(ring)
        delay(FLASH_MS)
    }
    delay(FINISH_HOLD_MS)
    val start = SystemClock.uptimeMillis()
    while (true) {
        val faded = 1f - (SystemClock.uptimeMillis() - start).toFloat() / FINISH_FADE_MS
        if (faded <= 0f) break
        emit(fill.frame().scaled(faded))
        delay(METER_TICK_MS)
    }
    emit(dark)
}
