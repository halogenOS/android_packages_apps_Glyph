/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.output

import custom.glyph.frame.Frame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update

/**
 * The picture currently on the Glyphs, as the preview shows it, and the levels of the indicator
 * LEDs beside it. [drive] hands both to the hardware dimmed to the current brightness, unless
 * the picture asked for full brightness.
 */
class GlyphLights(val size: Int) {

    private data class Lit(val frame: Frame, val fullBrightness: Boolean)

    private val current = MutableStateFlow(Lit(Frame.dark(size), fullBrightness = false))

    private val picture = MutableStateFlow(Frame.dark(size))

    val frame: StateFlow<Frame> = picture.asStateFlow()

    private val indicators = MutableStateFlow(emptyMap<String, Float>())

    /** Whether anything is lit, which is when sensors that tune the output are worth running. */
    val lit: Flow<Boolean> = combine(picture, indicators) { frame, levels -> !frame.isDark || levels.values.any { it > 0f } }
        .distinctUntilChanged()

    fun show(frame: Frame, fullBrightness: Boolean = false) {
        current.value = Lit(frame, fullBrightness)
        picture.value = frame
    }

    fun clear() = show(Frame.dark(size))

    /** Holds the indicator called [name] at each of [levels] while the caller waits, and turns it off afterwards. */
    suspend fun light(name: String, levels: Flow<Float>) {
        try {
            levels.collect { level -> indicators.update { it + (name to level.coerceIn(0f, 1f)) } }
        } finally {
            indicators.update { it - name }
        }
    }

    /** Sends every picture to [output] for as long as the caller runs. */
    suspend fun drive(output: GlyphOutput, brightness: Flow<Float>) {
        combine(current, indicators, brightness) { lit, levels, level ->
            (if (lit.fullBrightness) lit.frame else lit.frame.scaled(level)) to levels.mapValues { it.value * level }
        }.collect { (frame, levels) -> output.show(frame, levels) }
    }
}
