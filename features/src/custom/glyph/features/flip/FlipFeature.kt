/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.flip

import android.content.Context
import android.media.AudioManager
import custom.glyph.animation.frames
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Preemption
import custom.glyph.arbiter.Priority
import custom.glyph.feature.GlyphFeature
import custom.glyph.glyph
import custom.glyph.platform.internalRingerMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.withIndex

/**
 * Laying the phone face down plays the flip animation and switches the ringer to the chosen
 * mode; picking it up again brings the earlier mode back, unless the user changed it meanwhile.
 * The earlier mode is stored, so it is restored even after the app restarted in between.
 */
class FlipFeature(private val context: Context) : GlyphFeature {

    private val glyph = context.glyph
    private val settings = glyph.settings
    private val audio = context.getSystemService(AudioManager::class.java)

    override suspend fun run() {
        settings[FlipSettings.enabled].collectLatest { enabled ->
            if (enabled) followFlips() else restoreRinger()
        }
    }

    private suspend fun followFlips() {
        context.faceDown()
            .withIndex()
            .collect { (index, faceDown) ->
                if (faceDown) {
                    applyRinger()
                    if (index > 0) playFlipAnimation()
                } else {
                    restoreRinger()
                }
            }
    }

    private suspend fun applyRinger() {
        val mode = settings[FlipSettings.ringerMode].first()
        if (settings[FlipSettings.ringerBefore].first() != FlipSettings.NONE) return
        settings.set(FlipSettings.ringerBefore, audio.internalRingerMode)
        settings.set(FlipSettings.ringerApplied, mode)
        audio.internalRingerMode = mode
    }

    private suspend fun restoreRinger() {
        val before = settings[FlipSettings.ringerBefore].first()
        if (before == FlipSettings.NONE) return
        if (audio.internalRingerMode == settings[FlipSettings.ringerApplied].first()) audio.internalRingerMode = before
        settings.set(FlipSettings.ringerBefore, FlipSettings.NONE)
        settings.set(FlipSettings.ringerApplied, FlipSettings.NONE)
    }

    private suspend fun playFlipAnimation() {
        val animation = glyph.library.entry(FLIP_ANIMATION)?.animation ?: return
        glyph.arbiter.show(GlyphRequest(SOURCE, Priority.NOTIFICATION, animation.frames(), Preemption.DROP))
    }

    private companion object {
        const val SOURCE = "flip"
        const val FLIP_ANIMATION = "system/flip"
    }
}
