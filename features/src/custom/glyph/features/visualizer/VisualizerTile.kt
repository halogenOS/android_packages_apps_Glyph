/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.visualizer

import android.service.quicksettings.Tile
import custom.glyph.features.R
import custom.glyph.glyph
import custom.glyph.ui.tile.StateTile
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Turns the music visualizer on and off; it is unavailable while the Glyphs are off or paused. */
class VisualizerTile : StateTile<VisualizerTile.Shown>() {

    data class Shown(val enabled: Boolean, val quiet: Boolean)

    override fun states() = combine(glyph.settings[VisualizerSettings.enabled], glyph.quietRules) { enabled, rules -> Shown(enabled, rules.quiet) }

    override fun onClick() {
        scope.launch { glyph.settings.set(VisualizerSettings.enabled, !glyph.settings[VisualizerSettings.enabled].first()) }
    }

    override fun Tile.show(state: Shown) {
        this.state = when {
            state.quiet -> Tile.STATE_UNAVAILABLE
            state.enabled -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        subtitle = getString(
            when {
                state.quiet -> R.string.unavailable
                state.enabled -> R.string.on
                else -> R.string.off
            }
        )
    }
}
