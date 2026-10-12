/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.device

import custom.glyph.signal.readNode

/** The brightness values for this phone's back colour, falling back when the colour is unknown. */
fun GlyphDevice.Brightness.shell(): GlyphDevice.Shell {
    val color = shellColorNode?.let(::readNode)?.lowercase()
    return shells[color] ?: shells.getValue(fallbackShell)
}

/**
 * The auto brightness for one back light sensor event, as a fraction of full brightness: the
 * step for the coarse level the event reports, held up to the next step.
 */
fun GlyphDevice.Brightness.autoLevel(shell: GlyphDevice.Shell, values: FloatArray): Float {
    val level = values.getOrElse(levelValue) { 0f }
    val step = shell.auto.lastOrNull { level >= it.sensorLevel } ?: shell.auto.first()
    return step.brightness.toFloat() / scale
}
