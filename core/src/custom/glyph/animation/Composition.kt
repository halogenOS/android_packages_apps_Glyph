/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.animation

import android.util.Base64
import custom.glyph.device.GlyphDevice
import custom.glyph.frame.Frame
import java.util.zip.InflaterInputStream
import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * The lights of a Glyph Composer ringtone. Its AUTHOR tag holds Base64 of zlib data: one row of
 * comma-terminated levels per 1/60 s, each row as wide as the phone it was made for has
 * channels. The rows become an animation for this [device] that lasts as long as the audio:
 * rows past its end are dropped and a shorter one is held dark until it, so that the lights
 * and the sound loop together.
 */
object Composition {

    private const val FRAMES_PER_SECOND = 60.0

    /** The animation the [author] data describes, or null when it holds nothing this device can show. */
    fun animation(author: String, name: String, audioMillis: Long, device: GlyphDevice): Animation? {
        val frames = rows(author)?.take(rowsWithin(audioMillis))?.map { device.foreignLayouts.forRow(it, device.frame.size) ?: return null }
        if (frames.isNullOrEmpty()) return null
        val steps = frames.runs().map { Animation.Step(it.endRow.millis - it.firstRow.millis, it.frame.toList()) }
        val lightsMillis = frames.size.millis
        val tail = if (audioMillis > lightsMillis) listOf(Animation.Step(audioMillis - lightsMillis, Frame.dark(device.frame.size).toList())) else emptyList()
        return Animation(Animation.FORMAT, device.codename, name, steps + tail)
    }

    private fun rows(author: String): List<IntArray>? = runCatching {
        InflaterInputStream(Base64.decode(author.trim(), Base64.DEFAULT).inputStream()).bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() }
                .map { line -> line.split(',').filter { it.isNotBlank() }.map { it.trim().toInt() }.toIntArray() }
                .toList()
        }
    }.getOrNull()

    /** One [frame] shown from [firstRow] until [endRow] begins. */
    private class Run(val frame: Frame, val firstRow: Int, val endRow: Int)

    /** Consecutive equal frames merged into runs. */
    private fun List<Frame>.runs(): List<Run> {
        val starts = indices.filter { it == 0 || this[it] != this[it - 1] }
        return starts.zip(starts.drop(1) + size) { first, end -> Run(this[first], first, end) }
    }

    /** How many rows start before the audio ends; light data past the audio is never played. */
    private fun rowsWithin(audioMillis: Long): Int =
        if (audioMillis > 0) ceil(audioMillis * FRAMES_PER_SECOND / 1000).toInt() else Int.MAX_VALUE

    /** When [this] row starts, rounded so that rounding never adds up over long compositions. */
    private val Int.millis: Long get() = (this * 1000 / FRAMES_PER_SECOND).roundToLong()
}
