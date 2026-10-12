/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import android.content.Context
import android.os.PowerManager
import custom.glyph.feature.GlyphFeature
import custom.glyph.features.flip.faceDown
import custom.glyph.platform.sleepNow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take

/** Laying the phone face down while the Glyph Timer is ready starts it and turns the screen off. */
class FlipToStart(private val context: Context) : GlyphFeature {

    private val timer = context.glyphTimer
    private val power = context.getSystemService(PowerManager::class.java)

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun run() {
        timer.state
            .map { it is TimerState.Ready }
            .distinctUntilChanged()
            .flatMapLatest { ready -> if (ready) context.faceDown().filter { it }.take(1) else emptyFlow() }
            .collect {
                timer.start()
                power.sleepNow()
            }
    }
}
