/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.device

import custom.glyph.frame.Frame
import kotlinx.serialization.Serializable

/**
 * How light data made for Nothing phones maps onto this device. Glyph Developer Kit apps and
 * Glyph Composer ringtones both send one level from 0 to [MAX_LEVEL] per channel. Data for one
 * of this device's own [models], or as many channels as its frame has, is in frame order
 * already; data for another phone is carried over by its [layouts] entry, found by the model an
 * app names or by how many channels a ringtone row has.
 */
@Serializable
data class ForeignLayouts(
    val models: List<String> = emptyList(),
    val layouts: List<Layout> = emptyList(),
) {

    /** Data with [channels] values per frame from another phone; each LED takes the brightest of its [sources]. */
    @Serializable
    data class Layout(
        val name: String,
        val channels: Int,
        val sources: List<List<Int>>,
        val models: List<String> = emptyList(),
    ) {
        fun frame(levels: IntArray): Frame = Frame.of(sources.map { from -> from.maxOfOrNull { levels[it] }?.fraction ?: 0f })
    }

    /** The frame [levels] stand for when an app made for [model] sends them, or null when they fit nothing. */
    fun forModel(levels: IntArray, model: String?, size: Int): Frame? =
        model?.takeUnless { it in models }
            ?.let { named -> layouts.firstOrNull { named in it.models } }
            ?.takeIf { levels.size >= it.channels }
            ?.frame(levels)
            ?: native(levels, size)

    /** The frame a row of [levels] stands for, judged by how many channels it has, or null when none fits. */
    fun forRow(levels: IntArray, size: Int): Frame? =
        native(levels, size) ?: layouts.firstOrNull { it.channels == levels.size }?.frame(levels)

    private fun native(levels: IntArray, size: Int): Frame? =
        levels.takeIf { it.size == size }?.let { Frame.of(it.map { level -> level.fraction }) }

    companion object {
        const val MAX_LEVEL = 4095

        private val Int.fraction: Float get() = toFloat() / MAX_LEVEL
    }
}
