/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.torch

import android.content.Context
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Priority
import custom.glyph.feature.GlyphFeature
import custom.glyph.frame.Frame
import custom.glyph.glyph
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow

/** Lights every Glyph at full brightness above everything else while the torch is on, even when the Glyphs are off. */
class TorchFeature(private val context: Context) : GlyphFeature {

    private val glyph = context.glyph

    private val request = GlyphRequest(
        source = SOURCE,
        priority = Priority.TORCH,
        frames = flow { emit(Frame.full(glyph.device.frame.size)); awaitCancellation() },
        ignoresQuietRules = true,
        fullBrightness = true,
        animated = false,
    )

    override suspend fun run() = context.glyphTorch.on.collectLatest { on -> if (on) glyph.arbiter.show(request) }

    private companion object {
        const val SOURCE = "torch"
    }
}
