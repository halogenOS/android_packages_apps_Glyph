/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import android.content.Context
import custom.glyph.features.timer.TimerState.Idle
import custom.glyph.features.timer.TimerState.Paused
import custom.glyph.features.timer.TimerState.Ready
import custom.glyph.features.timer.TimerState.Ringing
import custom.glyph.features.timer.TimerState.Running
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The timer of the Glyph Timer tile: it gets ready, starts once the phone lies face down,
 * counts down and rings. Every change is a step from one [TimerState] to the next; a step
 * that does not fit the current state does nothing.
 */
class GlyphTimer {

    private val current = MutableStateFlow<TimerState>(Idle)

    val state: StateFlow<TimerState> = current.asStateFlow()

    /** What tapping the tile does: get ready, cancel, pause, resume or end the ringing. */
    fun toggle(totalMillis: Long) = current.update { state ->
        when (state) {
            Idle -> Ready(totalMillis)
            is Ready -> Idle
            is Running -> state.paused()
            is Paused -> state.resumed()
            Ringing -> Idle
        }
    }

    fun start() = current.update { if (it is Ready) it.started() else it }

    fun pause() = current.update { if (it is Running) it.paused() else it }

    fun resume() = current.update { if (it is Paused) it.resumed() else it }

    fun expire() = current.update { if (it is Running) Ringing else it }

    fun cancelIfReady() = current.update { if (it is Ready) Idle else it }

    fun stop() {
        current.value = Idle
    }

    /** Implemented by the application, so every component of the process reaches the same timer. */
    interface Host {
        val glyphTimer: GlyphTimer
    }
}

val Context.glyphTimer: GlyphTimer get() = (applicationContext as GlyphTimer.Host).glyphTimer
