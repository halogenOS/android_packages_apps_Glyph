/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.signal

import android.content.Context
import android.media.AudioManager
import custom.glyph.platform.EXTRA_PREVIOUS_VOLUME
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/** A stream's volume moved from [previous] to [level], both from 0 (lowest) to 1 (highest). */
data class VolumeChange(val stream: Int, val previous: Float, val level: Float)

/** Every change of one of these [streams]' volume from now on, as the system announces it. */
fun Context.volumeChanges(streams: Set<Int>): Flow<VolumeChange> {
    val audio = getSystemService(AudioManager::class.java)
    return broadcasts(AudioManager.ACTION_VOLUME_CHANGED, skipSticky = true).mapNotNull { intent ->
        val stream = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1)
        val index = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, -1)
        val previous = intent.getIntExtra(EXTRA_PREVIOUS_VOLUME, -1)
        if (stream !in streams || index < 0 || previous < 0) return@mapNotNull null
        val min = audio.getStreamMinVolume(stream)
        val range = (audio.getStreamMaxVolume(stream) - min).takeIf { it > 0 } ?: return@mapNotNull null
        VolumeChange(stream, (previous - min).toFloat() / range, (index - min).toFloat() / range)
    }
}
