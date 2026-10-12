/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.calls

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.telephony.TelephonyManager
import custom.glyph.animation.looping
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Priority
import custom.glyph.feature.GlyphFeature
import custom.glyph.glyph
import custom.glyph.signal.audioMode
import custom.glyph.signal.callState
import custom.glyph.signal.playbackActive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Plays an animation over and over while the phone rings: the lights of the ringtone when it is
 * a Glyph Composer ringtone and they are wanted, otherwise the chosen call animation.
 */
class CallFeature(private val context: Context) : GlyphFeature {

    private val glyph = context.glyph

    override suspend fun run() {
        combine(glyph.settings[CallSettings.enabled], glyph.settings[CallSettings.animation], glyph.settings[CallSettings.ringtoneLights], ::Triple)
            .collectLatest { (enabled, animationId, ringtoneLights) -> if (enabled) lightWhileRinging(animationId, ringtoneLights) }
    }

    private suspend fun lightWhileRinging(animationId: String, ringtoneLights: Boolean) {
        val animation = (glyph.library.entry(animationId) ?: glyph.library.entry(CallSettings.animation.default))?.animation ?: return
        ringing().collectLatest { ringing ->
            if (!ringing) return@collectLatest
            val lights = ringtone().takeIf { ringtoneLights }?.let { glyph.library.compositions.animation(it) }
            if (lights != null) ringtoneStarted()
            glyph.arbiter.show(GlyphRequest(SOURCE, Priority.CALL, (lights ?: animation).looping()))
        }
    }

    private fun ringtone(): Uri? = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)

    /** Waits until the ringtone sounds, so its lights start with it; a phone that only vibrates waits briefly. */
    private suspend fun ringtoneStarted() {
        withTimeoutOrNull(RINGTONE_START_TIMEOUT_MS) { context.playbackActive(RINGTONE_USAGES).first { it } }
    }

    /**
     * Whether a call is ringing. It ends early once the ringtone has played and stopped, as
     * when the ringer is silenced; in silent or vibrate mode it lasts until the call is taken
     * or ends.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun ringing(): Flow<Boolean> = context.callState()
        .map { it == TelephonyManager.CALL_STATE_RINGING }
        .distinctUntilChanged()
        .flatMapLatest { ringing ->
            if (!ringing) {
                flowOf(false)
            } else {
                flow {
                    emit(true)
                    context.audioMode().dropWhile { it != AudioManager.MODE_RINGTONE }.first { it != AudioManager.MODE_RINGTONE }
                    emit(false)
                }
            }
        }
        .distinctUntilChanged()

    private companion object {
        const val SOURCE = "call"
        const val RINGTONE_START_TIMEOUT_MS = 1_000L
        val RINGTONE_USAGES = setOf(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
    }
}
