/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.signal

import android.content.Context
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Whether anything audibly plays with one of these [usages], right away and on every change. */
fun Context.playbackActive(usages: Set<Int>): Flow<Boolean> = callbackFlow {
    val audio = getSystemService(AudioManager::class.java)
    fun report(configs: List<AudioPlaybackConfiguration>) {
        trySend(configs.any { it.isActive && it.audioAttributes.usage in usages })
    }
    val callback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) = report(configs)
    }
    audio.registerAudioPlaybackCallback(callback, null)
    report(audio.activePlaybackConfigurations)
    awaitClose { audio.unregisterAudioPlaybackCallback(callback) }
}.distinctUntilChanged()
