/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.feature

/**
 * Something the Glyphs do on their own, such as showing the battery level when a charger is
 * plugged in. It runs for the life of the process, follows its own settings and asks the
 * arbiter for the Glyphs whenever it has something to show.
 */
fun interface GlyphFeature {
    suspend fun run()
}
