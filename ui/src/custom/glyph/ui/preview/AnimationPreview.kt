/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import custom.glyph.Glyph
import custom.glyph.animation.AnimationEntry
import custom.glyph.animation.frames
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Preemption
import custom.glyph.arbiter.Priority
import custom.glyph.glyph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Plays one animation at a time on the Glyphs as a preview, for as long as [scope] lives. */
class AnimationPreview(private val glyph: Glyph, private val scope: CoroutineScope) {

    private val current = MutableStateFlow<AnimationEntry?>(null)
    val playing: StateFlow<AnimationEntry?> = current.asStateFlow()

    private var job: Job? = null

    /** Plays [entry] once, or stops it when it is already playing. */
    fun toggle(entry: AnimationEntry) {
        val replay = current.value != entry
        stop()
        if (replay) play(entry)
    }

    /** Plays [entry] once from the start. */
    fun play(entry: AnimationEntry) {
        stop()
        current.value = entry
        job = scope.launch {
            try {
                glyph.arbiter.show(GlyphRequest(SOURCE, Priority.PREVIEW, entry.animation.frames(), Preemption.DROP))
            } finally {
                current.compareAndSet(entry, null)
            }
        }
    }

    fun stop() {
        job?.cancel()
        current.value = null
    }

    private companion object {
        const val SOURCE = "preview:animation"
    }
}

/** A preview that stops when the screen using it is left or hidden. */
@Composable
fun rememberAnimationPreview(): AnimationPreview {
    val glyph = LocalContext.current.glyph
    val scope = rememberCoroutineScope()
    val preview = remember { AnimationPreview(glyph, scope) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { preview.stop() }
    DisposableEffect(preview) { onDispose(preview::stop) }
    return preview
}
