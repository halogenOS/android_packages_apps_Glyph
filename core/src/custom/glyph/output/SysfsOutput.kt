/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.output

import android.util.Log
import custom.glyph.device.GlyphDevice
import custom.glyph.frame.Frame
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Writes frames to the LED driver's frame node as space separated hardware levels, and each
 * indicator's level to its own node whenever it changes. The chip is woken before anything is
 * lit, which may reset the indicators, so they are written again; it rests once the frame and
 * every indicator are dark.
 */
class SysfsOutput(
    private val node: GlyphDevice.FrameNode,
    private val indicators: Map<String, GlyphDevice.Indicator>,
) : GlyphOutput {

    private var written = emptyMap<String, Int>()
    private var awake = false

    init {
        node.setup.forEach { (path, value) -> write(path, value) }
    }

    override fun show(frame: Frame, indicators: Map<String, Float>) {
        val levels = this.indicators.mapValues { (name, indicator) -> ((indicators[name] ?: 0f) * indicator.maxLevel).roundToInt() }
        val lit = !frame.isDark || levels.values.any { it > 0 }
        val power = node.power
        if (power != null && lit) {
            write(power.node, power.active)
            if (!awake) written = emptyMap()
        }
        write(node.node, frame.toList().joinToString(" ") { (it * node.maxLevel).roundToInt().toString() })
        levels.filter { (name, level) -> written[name] != level }.forEach { (name, level) ->
            val indicator = this.indicators.getValue(name)
            write(indicator.node, "${indicator.channel} $level")
        }
        written = levels
        if (power != null && !lit) write(power.node, power.standby)
        awake = lit
    }

    private fun write(path: String, value: String) {
        try {
            File(path).writeText(value)
        } catch (e: IOException) {
            Log.e(TAG, "Cannot write $path", e)
        }
    }

    private companion object {
        const val TAG = "GlyphSysfsOutput"
    }
}
