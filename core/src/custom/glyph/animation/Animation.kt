/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.animation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A sequence of pictures for one device, as stored in a `.glyph` file. */
@Serializable
data class Animation(
    val format: Int,
    val device: String,
    val name: String,
    val steps: List<Step>,
) {
    val duration: Long get() = steps.sumOf { it.duration }

    /** One picture held for [duration] milliseconds, with a level from 0 to 1 per LED in frame order. */
    @Serializable
    data class Step(
        val duration: Long,
        val levels: List<Float>,
        val transition: Transition = Transition.HOLD,
    )

    enum class Transition {
        /** The step's picture appears at once. */
        @SerialName("hold") HOLD,

        /** The previous picture blends into this step's picture over the step's duration. */
        @SerialName("fade") FADE,
    }

    companion object {
        const val FORMAT = 1
    }
}
