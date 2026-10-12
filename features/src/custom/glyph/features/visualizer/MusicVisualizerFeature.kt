/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.visualizer

import android.content.Context
import android.media.AudioAttributes
import android.os.SystemClock
import android.util.Log
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Preemption
import custom.glyph.arbiter.Priority
import custom.glyph.device.GlyphDevice
import custom.glyph.feature.GlyphFeature
import custom.glyph.frame.Frame
import custom.glyph.glyph
import custom.glyph.signal.outputSpectrum
import custom.glyph.signal.outputSpectrumIntervalMillis
import custom.glyph.signal.playbackActive
import custom.glyph.ui.tile.offerTile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.produceIn

/** Lights the Glyphs to the beat of whatever music plays, for as long as it plays. */
class MusicVisualizerFeature(private val context: Context) : GlyphFeature {

    private val glyph = context.glyph

    override suspend fun run() {
        context.offerTile(VisualizerTile::class.java, isSupported(glyph.device))
        val layout = glyph.device.feature<VisualizerLayout>(LAYOUT) ?: return
        glyph.settings[VisualizerSettings.enabled].collectLatest { on ->
            if (!on) return@collectLatest
            context.playbackActive(MUSIC_USAGES).collectLatest { playing ->
                if (playing) glyph.arbiter.show(GlyphRequest(SOURCE, Priority.VISUALIZER, lightsFor(layout), Preemption.RESUME))
            }
        }
    }

    /** Frames at about 60 per second: captures make segments flash, the frames in between let them fade. */
    private fun lightsFor(layout: VisualizerLayout): Flow<Frame> = channelFlow {
        val pulses = MusicPulses(layout, outputSpectrumIntervalMillis)
        val captures = outputSpectrum()
            .catch { Log.e(TAG, "Cannot capture the audio output", it) }
            .produceIn(this)
        var shown: Frame? = null
        var last = SystemClock.uptimeMillis()
        while (true) {
            val now = SystemClock.uptimeMillis()
            pulses.fade(now - last)
            last = now
            captures.tryReceive().getOrNull()?.let { fft ->
                if (fft.isSilent()) pulses.silence() else pulses.capture(bandEnergies(fft, BANDS), now)
            }
            val frame = pulses.frame(glyph.device.frame.size)
            if (frame != shown) send(frame)
            shown = frame
            delay(FRAME_MS)
        }
    }

    companion object {
        /** Whether [device] describes how its Glyphs follow music. */
        fun isSupported(device: GlyphDevice) = LAYOUT in device.features

        private const val TAG = "GlyphMusicVisualizer"
        private const val SOURCE = "music_visualizer"
        private const val LAYOUT = "musicVisualizer"
        private const val BANDS = 100
        private const val FRAME_MS = 16L
        private val MUSIC_USAGES = setOf(AudioAttributes.USAGE_MEDIA)
    }
}
