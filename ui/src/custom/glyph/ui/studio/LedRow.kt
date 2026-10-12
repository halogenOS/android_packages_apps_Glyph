/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import custom.glyph.ui.preview.PreviewColors

/**
 * The LEDs of one strip side by side, large enough to pick one by one. Each box shows its LED's
 * level and is outlined while picked. A tap toggles one LED; a drag picks every LED between
 * where it started and where the finger is.
 */
@Composable
fun LedRow(
    leds: List<Int>,
    levels: List<Float>,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    onPick: (Collection<Int>) -> Unit,
    modifier: Modifier = Modifier,
    colors: PreviewColors = PreviewColors(),
) {
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = modifier.fillMaxWidth().height(BOX_HEIGHT)
            .pointerInput(leds) {
                detectTapGestures { onToggle(leds[leds.indexAt(it, size.width)]) }
            }
            .pointerInput(leds) {
                var start = 0
                detectDragGestures(onDragStart = { start = leds.indexAt(it, size.width); onPick(listOf(leds[start])) }) { change, _ ->
                    val end = leds.indexAt(change.position, size.width)
                    onPick(leds.subList(minOf(start, end), maxOf(start, end) + 1))
                }
            },
        horizontalArrangement = Arrangement.spacedBy(GAP),
    ) {
        leds.forEach { led ->
            Box(
                Modifier.weight(1f).height(BOX_HEIGHT)
                    .background(lerp(colors.off, colors.on, levels[led]), shape)
                    .then(if (led in selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier),
            )
        }
    }
}

/** The box under [position] in a row [width] pixels wide. */
private fun List<Int>.indexAt(position: Offset, width: Int): Int = (position.x / width * size).toInt().coerceIn(indices)

private val BOX_HEIGHT = 40.dp
private val GAP = 3.dp
