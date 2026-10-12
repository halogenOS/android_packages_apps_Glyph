/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.RingtoneManager
import android.provider.Settings
import custom.glyph.feature.GlyphFeature
import custom.glyph.glyph
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Plays the alarm sound over and over while the Glyph Timer rings, unless the alarm is muted. */
class TimerSound(private val context: Context) : GlyphFeature {

    private val audio = context.getSystemService(AudioManager::class.java)

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    override suspend fun run() {
        combine(context.glyphTimer.state, context.glyph.settings[TimerSettings.muteAlarm]) { state, muted ->
            state == TimerState.Ringing && !muted
        }
            .distinctUntilChanged()
            .collectLatest { ringing -> if (ringing) ring() }
    }

    private suspend fun ring() {
        val ringtone = RingtoneManager.getRingtone(context, Settings.System.DEFAULT_ALARM_ALERT_URI) ?: return
        ringtone.audioAttributes = attributes
        ringtone.isLooping = true
        val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attributes).build()
        audio.requestAudioFocus(focus)
        try {
            ringtone.play()
            awaitCancellation()
        } finally {
            ringtone.stop()
            audio.abandonAudioFocusRequest(focus)
        }
    }
}
