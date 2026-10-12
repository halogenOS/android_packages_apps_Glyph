/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.arbiter

import custom.glyph.frame.Frame
import kotlinx.coroutines.flow.Flow

/**
 * Something that wants the Glyphs. While it is the most important allowed request, its
 * [frames] are shown; it ends when they complete or when its holder stops waiting. Only an
 * [animated] request keeps the CPU awake while shown; a still light stays lit without it.
 */
class GlyphRequest(
    val source: String,
    val priority: Priority,
    val frames: Flow<Frame>,
    val preemption: Preemption = Preemption.RESUME,
    val ignoresQuietRules: Boolean = false,
    val fullBrightness: Boolean = false,
    val animated: Boolean = true,
) {
    override fun toString() = "GlyphRequest($source, $priority)"
}

/** From least to most important; a request of equal priority replaces the one before it. */
enum class Priority {
    SDK,
    VISUALIZER,
    PROGRESS,
    TIMER,
    ESSENTIAL,
    VOLUME,
    CHARGING,
    NOTIFICATION,
    TIMER_EXPIRED,
    PREVIEW,
    CALL,
    TORCH,
}

/** What happens to a request while a more important one is shown. */
enum class Preemption {
    /** It waits and starts showing again afterwards. */
    RESUME,

    /** It ends, so it never shows late. */
    DROP,
}
