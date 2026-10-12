/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import android.content.Context
import custom.glyph.feature.GlyphFeature
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** The Glyph Timer and the clock app's timers, each part following the timer on its own. */
class TimerFeature(context: Context) : GlyphFeature {

    private val parts = listOf(
        TimerLights(context),
        FlipToStart(context),
        TimerExpiry(context),
        TimerSound(context),
        TimerNotifications(context),
    )

    override suspend fun run() = coroutineScope {
        parts.forEach { launch { it.run() } }
    }
}
