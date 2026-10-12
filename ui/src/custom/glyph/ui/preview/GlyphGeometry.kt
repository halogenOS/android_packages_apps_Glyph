/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.preview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.vector.PathParser
import custom.glyph.device.GlyphDevice

/** The device's zones resolved into drawable shapes, with every LED as its own stretch of path. */
class GlyphGeometry(device: GlyphDevice) {

    val size = Size(device.outline.width, device.outline.height)
    val cornerRadius = device.outline.cornerRadius
    val stroke = device.stroke

    /** How far from an LED a touch still counts as touching it, in outline units. */
    val touchReach = stroke * TOUCH_REACH
    val zones = device.zones.map(::ZoneShape)

    /** The zone whose LEDs lie closest to [point], if any lies within [reach]. */
    fun zoneAt(point: Offset, reach: Float): GlyphDevice.Zone? =
        zones.map { it to it.distanceTo(point) }
            .filter { (_, distance) -> distance <= reach }
            .minByOrNull { (_, distance) -> distance }
            ?.first?.zone

    class ZoneShape(val zone: GlyphDevice.Zone) {
        val path = PathParser().parsePathString(zone.path).toPath()

        private val measure = PathMeasure().apply { setPath(path, false) }

        val start: Offset = measure.getPosition(0f)
        val end: Offset = measure.getPosition(measure.length)

        val leds: List<LedShape> = run {
            val step = measure.length / zone.leds.size
            zone.leds.mapIndexed { position, led -> LedShape(led, measure, step * position, step * (position + 1)) }
        }

        fun distanceTo(point: Offset): Float = leds.minOf { it.distanceTo(point) }
    }

    /** One LED's stretch of its zone, drawn slightly into its neighbours so no anti-aliased seam shows between them. */
    class LedShape(val index: Int, measure: PathMeasure, from: Float, to: Float) {
        val path = Path().also {
            measure.getSegment((from - SEAM_OVERLAP).coerceAtLeast(0f), (to + SEAM_OVERLAP).coerceAtMost(measure.length), it, startWithMoveTo = true)
        }
        val samples = (0..SAMPLES).map { measure.getPosition(from + (to - from) * it / SAMPLES) }

        fun distanceTo(point: Offset): Float = samples.minOf { (it - point).getDistance() }
    }

    private companion object {
        const val SAMPLES = 8
        const val TOUCH_REACH = 3f
        const val SEAM_OVERLAP = 0.4f
    }
}
