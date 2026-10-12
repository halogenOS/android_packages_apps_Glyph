/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.progress

/**
 * What the progress ring follows: [fraction] of the way done for the source named [key], with
 * [milestones] that flash the ring when reached. When [completes], reaching the end plays the
 * finishing animation and ends the display.
 */
data class TrackedProgress(
    val key: String,
    val fraction: Float,
    val milestones: List<Float> = emptyList(),
    val completes: Boolean = true,
)
