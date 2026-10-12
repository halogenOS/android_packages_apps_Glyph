/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.visualizer

import kotlinx.serialization.Serializable

/**
 * How a device's Glyphs follow music: every [Segment] listens to a few spectrum bands and lights
 * its LEDs, never dimmer than [minLevel] while lit.
 */
@Serializable
data class VisualizerLayout(val minLevel: Float, val segments: List<Segment>) {

    /**
     * Listens to [bandCount] bands from [firstBand] on and lights [leds]. A capture at least
     * [hitRatio] of the loudest one so far flashes it fully; between flashes it fades by [fade]
     * per capture. After [holdMillis] without a flash, the loudest level drops by [peakFade] per
     * capture, so quieter passages flash too.
     */
    @Serializable
    data class Segment(
        val firstBand: Int,
        val bandCount: Int,
        val leds: List<Int>,
        val hitRatio: Float,
        val fade: Float,
        val holdMillis: Long,
        val peakFade: Float,
    )
}
