/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import custom.glyph.Glyph
import custom.glyph.animation.Animation
import custom.glyph.animation.looping
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Priority
import custom.glyph.device.GlyphDevice
import custom.glyph.frame.Frame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One animation being edited: which step is open, which LEDs are picked, whether taps pick
 * [singleLeds], which [strip] lies open LED by LED, and whether it changed since it was saved.
 */
data class EditorState(
    val animation: Animation,
    val step: Int = 0,
    val selected: Set<Int> = emptySet(),
    val singleLeds: Boolean = false,
    val strip: GlyphDevice.Zone? = null,
    val playing: Boolean = false,
    val unsaved: Boolean = false,
) {
    val current: Animation.Step get() = animation.steps[step]

    val frame: Frame get() = Frame.of(current.levels)

    /** The level the picked LEDs share, or the brightest of them; null when none is picked. */
    val selectionLevel: Float? get() = selected.maxOfOrNull { current.levels[it] }
}

/** Edits the user's animation [id] and shows what is being edited on the Glyphs. */
class EditorViewModel(private val glyph: Glyph, private val id: String) : ViewModel() {

    private val editing = MutableStateFlow<EditorState?>(null)

    /** Null until the animation is read, and when it no longer exists. */
    val state: StateFlow<EditorState?> = editing.asStateFlow()

    val device: GlyphDevice get() = glyph.device

    init {
        viewModelScope.launch { editing.value = glyph.library.user.entry(id)?.animation?.let(::EditorState) }
    }

    fun openStep(step: Int) = edit { it.copy(step = step.coerceIn(it.animation.steps.indices)) }

    /**
     * A tap on [zone] in the drawing. It picks the whole zone or lets go of it; picking single
     * LEDs, it opens a strip LED by LED instead, and toggles a zone of one LED.
     */
    fun tapZone(zone: GlyphDevice.Zone) = when {
        editing.value?.singleLeds != true -> toggleZone(zone)
        zone.leds.size > 1 -> edit { it.copy(strip = zone) }
        else -> toggleLed(zone.leds.single())
    }

    /** Picks every LED of [zone], or lets go of them when all are picked already. */
    private fun toggleZone(zone: GlyphDevice.Zone) = change { state ->
        val leds = zone.leds.toSet()
        if (state.selected.containsAll(leds)) state.copy(selected = state.selected - leds) else state.picking(leds)
    }

    fun toggleLed(led: Int) = change { state ->
        if (led in state.selected) state.copy(selected = state.selected - led) else state.picking(setOf(led))
    }

    fun addLeds(leds: Collection<Int>) = change { it.picking(leds) }

    fun clearSelection() = edit { it.copy(selected = emptySet()) }

    fun setSingleLeds(single: Boolean) = edit { it.copy(singleLeds = single, strip = null) }

    fun setLevel(level: Float) = change { state ->
        state.withStep(state.current.copy(levels = state.current.levels.mapIndexed { led, old -> if (led in state.selected) level else old }))
    }

    fun setDuration(millis: Long) = change { it.withStep(it.current.copy(duration = millis.coerceIn(MIN_STEP_MS, MAX_STEP_MS))) }

    fun setFade(fade: Boolean) = change {
        it.withStep(it.current.copy(transition = if (fade) Animation.Transition.FADE else Animation.Transition.HOLD))
    }

    /** Adds a copy of the open step right after it and opens the copy. */
    fun addStep() = change { state ->
        val steps = state.animation.steps.toMutableList().apply { add(state.step + 1, state.current) }
        state.copy(animation = state.animation.copy(steps = steps), step = state.step + 1)
    }

    fun deleteStep() = change { state ->
        if (state.animation.steps.size <= 1) return@change state
        val steps = state.animation.steps.toMutableList().apply { removeAt(state.step) }
        state.copy(animation = state.animation.copy(steps = steps), step = state.step.coerceAtMost(steps.lastIndex))
    }

    /** Moves the open step [by] places, staying within the animation. */
    fun moveStep(by: Int) = change { state ->
        val target = (state.step + by).coerceIn(state.animation.steps.indices)
        if (target == state.step) return@change state
        val steps = state.animation.steps.toMutableList().apply { add(target, removeAt(state.step)) }
        state.copy(animation = state.animation.copy(steps = steps), step = target)
    }

    fun setPlaying(playing: Boolean) = edit { it.copy(playing = playing) }

    /** Stores the animation; [then] runs once it is written. */
    fun save(then: () -> Unit = {}) {
        val state = editing.value ?: return then()
        viewModelScope.launch {
            glyph.library.user.save(state.animation, id)
            editing.update { it?.copy(unsaved = it.animation != state.animation) }
            then()
        }
    }

    /** Shows the open step on the Glyphs, or the whole animation over and over while it plays, until cancelled. */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun showOnGlyphs() {
        val frames: Flow<Frame> = editing.filterNotNull()
            .map { state -> if (state.playing) Shown.Playing(state.animation) else Shown.Still(state.frame) }
            .distinctUntilChanged()
            .flatMapLatest { shown ->
                when (shown) {
                    is Shown.Playing -> shown.animation.looping()
                    is Shown.Still -> flowOf(shown.frame)
                }
            }
        glyph.arbiter.show(GlyphRequest(SOURCE, Priority.PREVIEW, frames))
    }

    private sealed interface Shown {
        data class Playing(val animation: Animation) : Shown
        data class Still(val frame: Frame) : Shown
    }

    private fun edit(block: (EditorState) -> EditorState) = editing.update { it?.let(block) }

    private fun change(block: (EditorState) -> EditorState) = edit { state ->
        block(state).let { changed -> if (changed.animation == state.animation) changed else changed.copy(unsaved = true) }
    }

    /**
     * Adds [leds] to the picked ones. Joining picked LEDs that already have a level, they take
     * that level, so the slider never shows a brightness the picked LEDs do not have.
     */
    private fun EditorState.picking(leds: Collection<Int>): EditorState {
        val level = selectionLevel
        val added = leds - selected
        val picked = copy(selected = selected + leds)
        return if (level == null || added.isEmpty()) picked else picked.withStep(current.copy(levels = current.levels.mapIndexed { led, old -> if (led in added) level else old }))
    }

    private fun EditorState.withStep(step: Animation.Step) =
        copy(animation = animation.copy(steps = animation.steps.toMutableList().apply { set(this@withStep.step, step) }))

    companion object {
        const val MIN_STEP_MS = 16L
        const val MAX_STEP_MS = 5_000L

        private const val SOURCE = "preview:studio"
    }
}
