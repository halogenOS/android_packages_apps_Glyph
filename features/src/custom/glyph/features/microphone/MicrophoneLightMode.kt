/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.microphone

import android.os.SystemClock
import androidx.annotation.StringRes
import custom.glyph.features.R
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.abs

private const val BLINK_MS = 800L
private const val BREATH_PERIOD_MS = 8_000L
private const val BREATH_MIN = 0.04f
private const val BREATH_TICK_MS = 50L

/** How the microphone light shows; one that moves keeps the phone awake while it does. */
enum class MicrophoneLightMode(@StringRes val label: Int, val moves: Boolean, val levels: () -> Flow<Float>) {

    BLINK(R.string.microphone_light_blink, moves = true, levels = {
        flow {
            while (true) {
                emit(1f)
                delay(BLINK_MS)
                emit(0f)
                delay(BLINK_MS)
            }
        }
    }),

    /** Fades between dim and full and back, as evenly as a triangle. */
    BREATHE(R.string.microphone_light_breathe, moves = true, levels = {
        flow {
            val start = SystemClock.uptimeMillis()
            while (true) {
                val phase = (SystemClock.uptimeMillis() - start) % BREATH_PERIOD_MS / BREATH_PERIOD_MS.toFloat()
                emit(BREATH_MIN + (1f - BREATH_MIN) * (1f - abs(2f * phase - 1f)))
                delay(BREATH_TICK_MS)
            }
        }
    }),

    STEADY(R.string.microphone_light_steady, moves = false, levels = {
        flow {
            emit(1f)
            awaitCancellation()
        }
    });

    companion object {
        fun named(name: String): MicrophoneLightMode = entries.firstOrNull { it.name == name } ?: BLINK
    }
}
