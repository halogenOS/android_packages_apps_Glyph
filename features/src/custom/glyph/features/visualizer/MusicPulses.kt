/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.visualizer

import custom.glyph.frame.Frame
import kotlin.math.max
import kotlin.math.pow

private const val CUTOFF = 0.05f

/**
 * How bright each segment of [layout] is while music plays. Captures arriving every
 * [captureMillis] make segments flash; [fade] lets them fade smoothly in between.
 */
class MusicPulses(private val layout: VisualizerLayout, private val captureMillis: Long) {

    private val segments = layout.segments
    private val peaks = FloatArray(segments.size)
    private val levels = FloatArray(segments.size)
    private val lastFlash = LongArray(segments.size)

    fun capture(bands: FloatArray, now: Long) = segments.forEachIndexed { index, segment ->
        val energy = (segment.firstBand until segment.firstBand + segment.bandCount).map { bands[it] }.average().toFloat()
        peaks[index] = max(peaks[index], energy)
        if (energy > 0f && energy >= peaks[index] * segment.hitRatio) {
            levels[index] = 1f
            lastFlash[index] = now
        }
        if (now - lastFlash[index] > segment.holdMillis) peaks[index] *= segment.peakFade
    }

    fun fade(elapsedMillis: Long) = segments.forEachIndexed { index, segment ->
        val faded = levels[index] * segment.fade.pow(elapsedMillis.toFloat() / captureMillis)
        levels[index] = if (faded < CUTOFF) 0f else faded
    }

    fun silence() {
        peaks.fill(0f)
        levels.fill(0f)
        lastFlash.fill(0L)
    }

    fun frame(size: Int): Frame = Frame.of(
        MutableList(size) { 0f }.apply {
            segments.forEachIndexed { index, segment ->
                val level = levels[index].takeIf { it > 0f }?.let { max(it, layout.minLevel) } ?: 0f
                segment.leds.forEach { this[it] = level }
            }
        }
    )
}
