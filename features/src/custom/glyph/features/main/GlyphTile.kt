/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.main

import android.service.quicksettings.Tile
import custom.glyph.arbiter.QuietRules
import custom.glyph.features.R
import custom.glyph.glyph
import custom.glyph.ui.tile.StateTile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Turns the Glyphs on and off, and tells while the schedule keeps them dark. */
class GlyphTile : StateTile<QuietRules>() {

    override fun states() = glyph.quietRules

    override fun onClick() {
        scope.launch { glyph.settings.set(glyph.core.enabled, !glyph.settings[glyph.core.enabled].first()) }
    }

    override fun Tile.show(state: QuietRules) {
        this.state = if (state.glyphsEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        subtitle = getString(
            when {
                !state.glyphsEnabled -> R.string.off
                state.scheduleActive -> R.string.paused_by_schedule
                else -> R.string.on
            }
        )
    }
}
