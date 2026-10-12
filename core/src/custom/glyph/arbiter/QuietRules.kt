/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.arbiter

/** Global conditions under which the Glyphs stay dark for everything but requests that ignore them. */
data class QuietRules(val glyphsEnabled: Boolean, val scheduleActive: Boolean) {

    val quiet: Boolean get() = !glyphsEnabled || scheduleActive

    fun allows(request: GlyphRequest): Boolean = !quiet || request.ignoresQuietRules
}
