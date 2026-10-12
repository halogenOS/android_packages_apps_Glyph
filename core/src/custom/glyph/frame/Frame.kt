/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.frame

/** One picture on the Glyphs: a level from 0 (off) to 1 (full) per LED, in kernel frame order. */
class Frame private constructor(private val levels: FloatArray) {

    val size: Int get() = levels.size

    val isDark: Boolean get() = levels.all { it == 0f }

    operator fun get(led: Int): Float = levels[led]

    fun with(leds: Iterable<Int>, level: Float): Frame =
        Frame(levels.copyOf().also { copy -> leds.forEach { copy[it] = level.coerceIn(0f, 1f) } })

    fun toList(): List<Float> = levels.asList()

    fun scaled(factor: Float): Frame = Frame(FloatArray(size) { (levels[it] * factor).coerceIn(0f, 1f) })

    override fun equals(other: Any?) = other is Frame && levels contentEquals other.levels

    override fun hashCode() = levels.contentHashCode()

    override fun toString() = levels.joinToString(prefix = "Frame(", postfix = ")")

    companion object {
        fun dark(size: Int) = Frame(FloatArray(size))

        fun full(size: Int) = Frame(FloatArray(size) { 1f })

        fun of(levels: List<Float>) = Frame(FloatArray(levels.size) { levels[it].coerceIn(0f, 1f) })
    }
}
