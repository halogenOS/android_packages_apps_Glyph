/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.visualizer

import kotlin.math.max
import kotlin.math.sqrt

private const val NOISE_FLOOR = 10f
private const val GAIN = 2.5f / 195f

/** Whether a capture carries no sound at all. */
fun ByteArray.isSilent(): Boolean = all { it == 0.toByte() }

/**
 * The lower half of an FFT capture split into [count] bands, each the average magnitude above a
 * small noise floor, roughly 0 to 1 for loud music.
 */
fun bandEnergies(fft: ByteArray, count: Int): FloatArray {
    val spectrum = fft.copyOfRange(2, fft.size / 2)
    val width = spectrum.size / count
    return FloatArray(count) { band ->
        val start = band * width
        val sum = (0 until width step 2).sumOf { offset ->
            val real = spectrum[start + offset].toFloat()
            val imaginary = spectrum.getOrElse(start + offset + 1) { 0 }.toFloat()
            max(sqrt(real * real + imaginary * imaginary) - NOISE_FLOOR, 0f).toDouble()
        }
        (sum / (width / 2f)).toFloat() * GAIN
    }
}
