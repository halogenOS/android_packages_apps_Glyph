/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.output

import android.os.SystemClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

private const val SENSOR_LINGER_MS = 2_000L
private const val FADE_MS = 1_000L
private const val FADE_STEP_MS = 16L

/**
 * The brightness the Glyphs are driven at, as a fraction of full brightness.
 *
 * A manual level applies at once. With auto brightness the back light sensor runs while
 * something is lit, staying on through the short dark gaps of an animation, and the Glyphs fade
 * to the step of each new reading.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
fun brightnessLevel(
    manual: Flow<Float>,
    auto: Flow<Boolean>,
    lit: Flow<Boolean>,
    backLight: Flow<FloatArray>,
    autoLevel: (FloatArray) -> Float,
    initialAuto: Float,
): Flow<Float> {
    var lastAuto = initialAuto
    val automatic = lit.debounce { on -> if (on) 0L else SENSOR_LINGER_MS }
        .distinctUntilChanged()
        .flatMapLatest { on -> if (on) backLight.map(autoLevel) else emptyFlow() }
        .onStart { emit(lastAuto) }
        .distinctUntilChanged()
        .onEach { lastAuto = it }
        .fading()
    return combine(auto, manual) { useAuto, level -> useAuto to level }
        .flatMapLatest { (useAuto, level) -> if (useAuto) automatic else flowOf(level) }
        .distinctUntilChanged()
}

/** Starts at the first value, then moves to each later one over [FADE_MS], from wherever it is. */
private fun Flow<Float>.fading(): Flow<Float> = channelFlow {
    var shown: Float? = null
    collectLatest { target ->
        val from = shown ?: target
        val start = SystemClock.uptimeMillis()
        do {
            val progress = ((SystemClock.uptimeMillis() - start).toFloat() / FADE_MS).coerceAtMost(1f)
            (from + (target - from) * progress).also { shown = it; send(it) }
            if (progress < 1f) delay(FADE_STEP_MS)
        } while (progress < 1f)
    }
}
