/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.torch

import android.service.quicksettings.Tile
import custom.glyph.features.R
import custom.glyph.ui.tile.StateTile

/** Turns the Glyph torch on and off. */
class TorchTile : StateTile<Boolean>() {

    override fun states() = glyphTorch.on

    override fun onClick() = glyphTorch.toggle()

    override fun Tile.show(state: Boolean) {
        this.state = if (state) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        subtitle = getString(if (state) R.string.on else R.string.off)
    }
}
