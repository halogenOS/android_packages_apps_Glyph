/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.animation

import custom.glyph.frame.Frame
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/** How often a meter takes a step, about 60 times a second. */
const val METER_TICK_MS = 16L

private const val EASE = 0.25f
private const val MIN_STEP = 0.05f

/**
 * How many of [leds] are lit, as a fraction that glides towards its goal: a quarter of the way
 * per tick, never slower than a twentieth of an LED, so it eases into place.
 */
class MeterFill(private val frameSize: Int, val leds: List<Int>) {

    var filled = 0f
        private set

    /** Moves one tick towards [goal] LEDs; false when it was there already. */
    fun stepTowards(goal: Float): Boolean {
        val diff = goal - filled
        if (diff == 0f) return false
        filled += diff.sign * min(abs(diff), max(abs(diff) * EASE, MIN_STEP))
        return true
    }

    fun frame(): Frame = meterFrame(frameSize, leds, filled)
}

/** The first [filled] of [leds] lit, the one at the edge by the fraction that reaches into it. */
fun meterFrame(frameSize: Int, leds: List<Int>, filled: Float): Frame = Frame.of(
    MutableList(frameSize) { 0f }.apply { leds.forEachIndexed { index, led -> this[led] = filled - index } }
)
