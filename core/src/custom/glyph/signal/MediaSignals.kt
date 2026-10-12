/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.signal

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

private const val POSITION_TICK_MS = 1_000L

/**
 * How far the playing song is, from 0 to 1, updated every second while it plays; null while
 * nothing with a known length plays. [listener] is the app's notification listener, which
 * entitles it to see the media sessions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Context.songPosition(listener: ComponentName): Flow<Float?> =
    mediaControllers(listener)
        .flatMapLatest { controllers ->
            if (controllers.isEmpty()) flowOf<Playback?>(null)
            else combine(controllers.map { it.playbackChanges() }) { states -> states.firstOrNull { it.isPlaying } }
        }
        .distinctUntilChanged()
        .flatMapLatest { playing ->
            if (playing == null) flowOf<Float?>(null)
            else flow<Float?> {
                while (true) {
                    emit(playing.fraction())
                    delay(POSITION_TICK_MS)
                }
            }
        }
        .distinctUntilChanged()

/** What one session plays and how, as of its last report. */
private data class Playback(val state: PlaybackState?, val duration: Long) {

    val isPlaying: Boolean get() = state?.state == PlaybackState.STATE_PLAYING && duration > 0

    /** The position now, carried forward from the last report at the playback speed. */
    fun fraction(): Float {
        val state = state ?: return 0f
        val elapsed = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
        val position = state.position + (elapsed * state.playbackSpeed).toLong()
        return (position.toFloat() / duration).coerceIn(0f, 1f)
    }
}

private fun Context.mediaControllers(listener: ComponentName): Flow<List<MediaController>> = callbackFlow {
    val sessions = getSystemService(MediaSessionManager::class.java)
    val changes = MediaSessionManager.OnActiveSessionsChangedListener { trySend(it.orEmpty()) }
    sessions.addOnActiveSessionsChangedListener(changes, listener, Handler(Looper.getMainLooper()))
    trySend(sessions.getActiveSessions(listener))
    awaitClose { sessions.removeOnActiveSessionsChangedListener(changes) }
}

private fun MediaController.playbackChanges(): Flow<Playback> = callbackFlow {
    fun report() {
        trySend(Playback(playbackState, metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L))
    }
    val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = report()
        override fun onMetadataChanged(metadata: MediaMetadata?) = report()
    }
    registerCallback(callback, Handler(Looper.getMainLooper()))
    report()
    awaitClose { unregisterCallback(callback) }
}
