/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.signal

import android.content.Context
import android.media.AudioManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** The phone's call state as a `TelephonyManager.CALL_STATE_*` value, starting with the current one. */
fun Context.callState(): Flow<Int> = callbackFlow {
    val telephony = getSystemService(TelephonyManager::class.java)
    val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            trySend(state)
        }
    }
    telephony.registerTelephonyCallback(Runnable::run, callback)
    awaitClose { telephony.unregisterTelephonyCallback(callback) }
}.distinctUntilChanged()

/** The audio mode as an `AudioManager.MODE_*` value, starting with the current one. */
fun Context.audioMode(): Flow<Int> = callbackFlow {
    val audio = getSystemService(AudioManager::class.java)
    val listener = AudioManager.OnModeChangedListener { trySend(it) }
    audio.addOnModeChangedListener(Runnable::run, listener)
    trySend(audio.mode)
    awaitClose { audio.removeOnModeChangedListener(listener) }
}.distinctUntilChanged()
