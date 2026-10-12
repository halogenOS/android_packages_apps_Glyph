/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.animation

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import custom.glyph.device.GlyphDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Glyph Composer ringtones as animations: the ones saved where the Composer keeps them, and
 * any sound that turns out to be one. A sound is read once and remembered, including when it
 * holds no lights, so ordinary ringtones cost nothing after the first look.
 */
class CompositionLibrary(private val context: Context, private val device: GlyphDevice) {

    private val read = LruCache<Uri, Read>(CACHE_SIZE)

    /** Every composition in the Composer's folder that this device can show. */
    suspend fun all(): List<AnimationEntry> = withContext(Dispatchers.IO) {
        saved().mapNotNull { (uri, name) -> animation(uri)?.let { AnimationEntry(idOf(uri), CATEGORY, it.copy(name = name)) } }
            .sortedBy { it.animation.name.lowercase() }
    }

    /** The composition an [id] from [all] names, or null when it is gone or no longer one. */
    suspend fun entry(id: String): AnimationEntry? =
        id.removePrefix(ID_PREFIX).takeIf { it != id }
            ?.let(Uri::parse)
            ?.let { uri -> animation(uri)?.let { AnimationEntry(id, CATEGORY, it) } }

    /**
     * The lights of the sound at [uri], or null when it is no composition. A default sound
     * setting stands for the sound it is set to. A sound that cannot be read now, as before the
     * storage is up, is tried again next time.
     */
    suspend fun animation(uri: Uri): Animation? = withContext(Dispatchers.IO) {
        val sound = if (RingtoneManager.isDefault(uri)) {
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.getDefaultType(uri)) ?: return@withContext null
        } else {
            uri
        }
        read[sound]?.let { return@withContext it.value }
        runCatching { decode(sound) }
            .onFailure { Log.w(TAG, "Cannot read $sound", it) }
            .getOrNull()
            ?.also { read.put(sound, it) }
            ?.value
    }

    private fun decode(sound: Uri): Read = MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(context, sound)
        val author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR) ?: return Read(null)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: sound.lastPathSegment.orEmpty()
        Read(Composition.animation(author, title, duration, device))
    }

    /** What a readable sound turned out to hold. */
    private class Read(val value: Animation?)

    /** The files in the Composer's folder, named as the Composer shows them: by file name. */
    private fun saved(): List<Pair<Uri, String>> {
        val columns = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
        val query = "${MediaStore.Audio.Media.RELATIVE_PATH} = ?"
        return context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns, query, arrayOf(FOLDER), null)
            ?.use { cursor ->
                generateSequence { cursor.takeIf { it.moveToNext() } }.map {
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it.getLong(0)) to it.getString(1).substringBeforeLast('.')
                }.toList()
            }
            .orEmpty()
    }

    private fun idOf(uri: Uri) = "$ID_PREFIX$uri"

    companion object {
        const val CATEGORY = "composition"

        private const val ID_PREFIX = "$CATEGORY:"
        private const val FOLDER = "Ringtones/Compositions/"
        private const val CACHE_SIZE = 32
        private const val TAG = "GlyphCompositions"
    }
}
