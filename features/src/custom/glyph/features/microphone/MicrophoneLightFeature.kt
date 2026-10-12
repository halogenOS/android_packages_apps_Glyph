/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.microphone

import android.content.Context
import android.os.PowerManager
import custom.glyph.device.GlyphDevice
import custom.glyph.feature.GlyphFeature
import custom.glyph.glyph
import custom.glyph.output.stayAwakeWhile
import custom.glyph.signal.microphoneUsers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Lights the recording indicator while one of the chosen apps uses the microphone, by the same
 * rules as the privacy indicator in the status bar. It stays dark whenever the Glyphs do.
 */
class MicrophoneLightFeature(private val context: Context) : GlyphFeature {

    private val glyph = context.glyph
    private val power = context.getSystemService(PowerManager::class.java)

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun run() {
        if (!isSupported(glyph.device)) return
        val settings = glyph.settings
        combine(settings[MicrophoneSettings.enabled], settings[MicrophoneSettings.apps], glyph.quietRules) { enabled, apps, rules ->
            apps.takeIf { enabled && !rules.quiet }.orEmpty()
        }
            .distinctUntilChanged()
            .flatMapLatest { apps -> if (apps.isEmpty()) flowOf(false) else context.microphoneUsers().map { users -> users.any(apps::contains) } }
            .distinctUntilChanged()
            .combine(settings[MicrophoneSettings.mode]) { inUse, mode -> MicrophoneLightMode.named(mode).takeIf { inUse } }
            .distinctUntilChanged()
            .collectLatest { mode -> if (mode != null) light(mode) }
    }

    private suspend fun light(mode: MicrophoneLightMode) = coroutineScope {
        if (mode.moves) launch { power.stayAwakeWhile(flow { emit(true); awaitCancellation() }, WAKE_TAG) }
        glyph.lights.light(INDICATOR, mode.levels())
    }

    companion object {
        private const val INDICATOR = "recording"
        private const val WAKE_TAG = "custom.glyph:microphone"

        fun isSupported(device: GlyphDevice) = INDICATOR in device.indicators
    }
}
