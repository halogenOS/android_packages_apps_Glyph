/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import android.service.quicksettings.Tile
import custom.glyph.features.R
import custom.glyph.glyph
import custom.glyph.ui.tile.StateTile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/**
 * The Glyph Timer tile. Each tap takes the timer a step further: ready, cancelled, paused,
 * resumed or silenced. Closing Quick Settings before the phone lies face down cancels a
 * ready timer.
 */
class TimerTile : StateTile<TimerTile.Shown>() {

    data class Shown(val state: TimerState, val durationMillis: Long)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun states() =
        combine(glyphTimer.state, glyph.settings[TimerSettings.durationSeconds], ::Pair).transformLatest { (state, seconds) ->
            while (true) {
                emit(Shown(state, seconds * 1000L))
                if (state !is TimerState.Running) break
                delay(SECOND_MS)
            }
        }

    override fun onStopListening() {
        super.onStopListening()
        glyphTimer.cancelIfReady()
    }

    override fun onClick() {
        scope.launch { glyphTimer.toggle(glyph.settings[TimerSettings.durationSeconds].first() * 1000L) }
    }

    override fun Tile.show(state: Shown) {
        val timer = state.state
        this.state = if (timer == TimerState.Idle) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        subtitle = when (timer) {
            TimerState.Idle -> timerText(state.durationMillis)
            is TimerState.Ready -> getString(R.string.timer_lay_face_down)
            is TimerState.Running -> timerText(timer.remaining())
            is TimerState.Paused -> getString(R.string.timer_paused_left, timerText(timer.remaining))
            TimerState.Ringing -> getString(R.string.timer_times_up)
        }
    }

    private companion object {
        const val SECOND_MS = 1_000L
    }
}
