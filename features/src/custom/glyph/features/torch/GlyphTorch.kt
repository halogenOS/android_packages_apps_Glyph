/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.torch

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Whether the Glyph torch is on. It starts off with every new process, like after a reboot. */
class GlyphTorch {

    private val current = MutableStateFlow(false)

    val on: StateFlow<Boolean> = current.asStateFlow()

    fun toggle() = current.update { !it }

    /** Implemented by the application so the tile and the lights share one torch. */
    interface Host {
        val glyphTorch: GlyphTorch
    }
}

val Context.glyphTorch: GlyphTorch get() = (applicationContext as GlyphTorch.Host).glyphTorch
