/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import custom.glyph.device.GlyphDevice
import custom.glyph.frame.Frame

@Immutable
data class PreviewColors(
    val body: Color = Color(0xFF1E1F22),
    val off: Color = Color(0xFF3A3B3F),
    val on: Color = Color.White,
    val glow: Color = Color.White,
    val selection: Color = Color(0xFF8AB4F8),
)

/**
 * The back of the phone with every LED drawn at the level [frame] gives it, and the [selected]
 * LEDs outlined. Tapping near a zone reports it through [onZoneTap].
 */
@Composable
fun GlyphPreview(
    geometry: GlyphGeometry,
    frame: Frame,
    modifier: Modifier = Modifier,
    colors: PreviewColors = PreviewColors(),
    selected: Set<Int> = emptySet(),
    onZoneTap: ((GlyphDevice.Zone) -> Unit)? = null,
) {
    val tap = onZoneTap?.let { callback ->
        Modifier.pointerInput(geometry, callback) {
            detectTapGestures { position ->
                val scale = size.width / geometry.size.width
                geometry.zoneAt(position / scale, reach = geometry.touchReach)?.let(callback)
            }
        }
    } ?: Modifier

    Canvas(modifier.aspectRatio(geometry.size.width / geometry.size.height, matchHeightConstraintsFirst = true).then(tap)) {
        scale(size.width / geometry.size.width, pivot = Offset.Zero) {
            drawRoundRect(colors.body, size = geometry.size, cornerRadius = CornerRadius(geometry.cornerRadius))
            if (selected.isNotEmpty()) {
                geometry.zones.forEach { zone ->
                    zone.leds.filter { it.index in selected }.forEach { led ->
                        drawPath(led.path, colors.selection, style = Stroke(geometry.stroke * SELECTION_WIDTH, cap = StrokeCap.Round))
                    }
                }
            }
            withLayer(Rect(Offset.Zero, geometry.size), alpha = GLOW_ALPHA) {
                geometry.zones.forEach { zone ->
                    drawStrip(zone, geometry.stroke * GLOW_WIDTH) { colors.glow.copy(alpha = frame[it.index]) }
                }
            }
            geometry.zones.forEach { zone ->
                drawStrip(zone, geometry.stroke) { lerp(colors.off, colors.on, frame[it.index]) }
            }
        }
    }
}

private inline fun DrawScope.withLayer(bounds: Rect, alpha: Float, block: DrawScope.() -> Unit) {
    drawIntoCanvas { it.saveLayer(bounds, Paint().apply { this.alpha = alpha }) }
    block()
    drawIntoCanvas { it.restore() }
}

/** Draws a zone LED by LED with flat joints between neighbours and round ends, so no part is painted twice. */
private fun DrawScope.drawStrip(zone: GlyphGeometry.ZoneShape, width: Float, colorOf: (GlyphGeometry.LedShape) -> Color) {
    zone.leds.forEach { led ->
        drawPath(led.path, colorOf(led), style = Stroke(width, cap = StrokeCap.Butt, join = StrokeJoin.Round))
    }
    drawCircle(colorOf(zone.leds.first()), radius = width / 2, center = zone.start)
    drawCircle(colorOf(zone.leds.last()), radius = width / 2, center = zone.end)
}

private const val SELECTION_WIDTH = 2f
private const val GLOW_WIDTH = 2.4f
private const val GLOW_ALPHA = 0.3f
