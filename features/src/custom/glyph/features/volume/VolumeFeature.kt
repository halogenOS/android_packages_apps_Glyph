/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.volume

import android.content.Context
import android.media.AudioManager
import custom.glyph.animation.liveMeterFrames
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Preemption
import custom.glyph.arbiter.Priority
import custom.glyph.feature.GlyphFeature
import custom.glyph.glyph
import custom.glyph.signal.volumeChanges
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** Shows the volume on the volume meter while it is being changed. */
class VolumeFeature(private val context: Context) : GlyphFeature {

    private val glyph = context.glyph

    override suspend fun run() {
        val meter = glyph.device.meters[VOLUME_METER] ?: return
        glyph.settings[VolumeSettings.enabled].collectLatest { on -> if (on) followVolume(meter) }
    }

    /** One meter follows every change; it fades out once the volume was left alone for a moment. */
    private suspend fun followVolume(meter: List<Int>) = coroutineScope {
        val level = MutableStateFlow(0f)
        var shown: Job? = null
        context.volumeChanges(SHOWN_STREAMS)
            .filter { it.level != it.previous }
            .collect { change ->
                level.value = change.level
                if (shown?.isActive != true) {
                    shown = launch {
                        val frames = liveMeterFrames(glyph.device.frame.size, meter, level, HOLD_MS)
                        glyph.arbiter.show(GlyphRequest(SOURCE, Priority.VOLUME, frames, Preemption.DROP))
                    }
                }
            }
    }

    private companion object {
        const val SOURCE = "volume"
        const val VOLUME_METER = "volume"
        const val HOLD_MS = 3_000L
        val SHOWN_STREAMS = setOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_ALARM,
            AudioManager.STREAM_VOICE_CALL,
        )
    }
}
