/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.output

import custom.glyph.frame.Frame

/** A destination for frames and indicator levels, such as the LED hardware. */
fun interface GlyphOutput {
    fun show(frame: Frame, indicators: Map<String, Float>)
}
