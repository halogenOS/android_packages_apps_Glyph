/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.animation

import kotlinx.serialization.json.Json

/** A `.glyph` file as it is exchanged between phones, and what one turns out to hold. */
object AnimationFile {

    const val EXTENSION = ".glyph"
    const val MIME_TYPE = "application/json"

    private val json = Json { ignoreUnknownKeys = true }

    sealed interface Read {
        data class Valid(val animation: Animation) : Read
        data object NotAnimation : Read
        data class OtherDevice(val device: String) : Read
        data object Newer : Read
    }

    fun encode(animation: Animation): ByteArray = json.encodeToString(animation).encodeToByteArray()

    /** What [bytes] hold, judged for a phone called [codename] whose frames have [frameSize] LEDs. */
    fun read(bytes: ByteArray, codename: String, frameSize: Int): Read {
        val animation = runCatching { json.decodeFromString<Animation>(bytes.decodeToString()) }.getOrNull()
        return when {
            animation == null || animation.steps.isEmpty() -> Read.NotAnimation
            animation.format > Animation.FORMAT -> Read.Newer
            animation.device != codename -> Read.OtherDevice(animation.device)
            animation.steps.any { it.levels.size != frameSize || it.duration < 0 } -> Read.NotAnimation
            else -> Read.Valid(animation)
        }
    }

    /** A file name for [name] that every file system accepts. */
    fun fileName(name: String): String = name.replace(UNSAFE, "_").trim().ifEmpty { "animation" } + EXTENSION

    private val UNSAFE = Regex("""[\\/:*?"<>|\p{Cntrl}]""")
}
