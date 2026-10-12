/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import android.content.Context
import custom.glyph.animation.frames
import custom.glyph.animation.looping
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Priority
import custom.glyph.feature.GlyphFeature
import custom.glyph.frame.Frame
import custom.glyph.glyph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Shows the Glyph Timer on the timer ring, or else the clock app's timer when that is wanted.
 * The Glyph Timer opens its countdown with the start animation; a timer whose time is up
 * plays the end pattern until it is dealt with.
 */
class TimerLights(private val context: Context) : GlyphFeature {

    private val glyph = context.glyph

    /** What the Glyphs show for a timer, and how. */
    private enum class Look(val priority: Priority?, val animated: Boolean) {
        DARK(null, false),

        /** Ready or paused: it blinks while someone looks, and may stand still once the phone sleeps. */
        WAITING(Priority.TIMER, false),
        STARTING(Priority.TIMER, true),
        COUNTING(Priority.TIMER, true),
        RINGING(Priority.TIMER_EXPIRED, true),
    }

    override suspend fun run() {
        val ring = glyph.device.meters[TIMER_METER] ?: return
        coroutineScope {
            val shown = combine(context.glyphTimer.state, clockTimer()) { own, clock -> if (own != TimerState.Idle) own else clock }
                .stateIn(this, SharingStarted.Eagerly, TimerState.Idle)
            shown.withPrevious()
                .map { (previous, current) -> lookOf(previous, current) }
                .distinctUntilChanged()
                .collectLatest { look ->
                    val priority = look.priority ?: return@collectLatest
                    val frames = framesFor(look, ring, shown) ?: return@collectLatest
                    glyph.arbiter.show(GlyphRequest(SOURCE, priority, frames, animated = look.animated))
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun clockTimer(): Flow<TimerState> =
        glyph.settings[TimerSettings.clockTimers].flatMapLatest { on -> if (on) context.clockTimer() else flowOf(TimerState.Idle) }

    private fun lookOf(previous: TimerState?, current: TimerState) = when (current) {
        TimerState.Idle -> Look.DARK
        is TimerState.Ready, is TimerState.Paused -> Look.WAITING
        is TimerState.Running -> if (previous is TimerState.Ready) Look.STARTING else Look.COUNTING
        TimerState.Ringing -> Look.RINGING
    }

    private suspend fun framesFor(look: Look, ring: List<Int>, shown: StateFlow<TimerState>): Flow<Frame>? {
        val countdown = timerFrames(glyph.device.frame.size, ring, shown)
        return when (look) {
            Look.DARK -> null
            Look.WAITING, Look.COUNTING -> countdown
            Look.STARTING -> animation(START_ANIMATION)?.let { start -> flow { emitAll(start.frames()); emitAll(countdown) } } ?: countdown
            Look.RINGING -> animation(END_ANIMATION)?.looping()
        }
    }

    private suspend fun animation(id: String) = glyph.library.entry(id)?.animation

    private companion object {
        const val SOURCE = "timer"
        const val TIMER_METER = "timer"
        const val START_ANIMATION = "system/timer_start"
        const val END_ANIMATION = "system/timer_end"
    }
}

/** Each value together with the one before it. */
private fun <T> Flow<T>.withPrevious(): Flow<Pair<T?, T>> = flow {
    var previous: T? = null
    collect { current ->
        emit(previous to current)
        previous = current
    }
}
