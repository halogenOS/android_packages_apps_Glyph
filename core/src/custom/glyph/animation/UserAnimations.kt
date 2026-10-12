/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.animation

import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * The animations the user made, one `.glyph` file each in [directory]. Every entry keeps the
 * id it was created with, so renaming one never loses where it was chosen. Only animations for
 * this device ([codename]) are offered.
 */
class UserAnimations(private val directory: File, private val codename: String) {

    private val json = Json { ignoreUnknownKeys = true }
    private val writing = Mutex()
    private val current = MutableStateFlow<List<AnimationEntry>?>(null)

    /** Every animation, sorted by name, now and after each change. */
    val entries: Flow<List<AnimationEntry>> = flow {
        all()
        emitAll(current.filterNotNull())
    }

    suspend fun all(): List<AnimationEntry> = current.value ?: writing.withLock { current.value ?: load().also { current.value = it } }

    suspend fun entry(id: String): AnimationEntry? = id.takeIf { it.startsWith(ID_PREFIX) }?.let { all().firstOrNull { entry -> entry.id == id } }

    /** Stores [animation] under [id], or as a new animation when [id] is null, and returns its entry. */
    suspend fun save(animation: Animation, id: String? = null): AnimationEntry = change {
        val entry = AnimationEntry(id ?: "$ID_PREFIX${UUID.randomUUID()}", CATEGORY, animation.copy(device = codename))
        write(entry)
        replacing(entry)
    }

    suspend fun rename(id: String, name: String): AnimationEntry? =
        entry(id)?.let { save(it.animation.copy(name = name), id) }

    suspend fun duplicate(id: String, name: String): AnimationEntry? =
        entry(id)?.let { save(it.animation.copy(name = name)) }

    /** Adds an animation from elsewhere under a new id, numbering its name when one of the user's already has it. */
    suspend fun import(animation: Animation): AnimationEntry {
        val taken = all().map { it.animation.name }.toSet()
        val numbered = generateSequence(2) { it + 1 }.map { "${animation.name} $it" }
        val name = (sequenceOf(animation.name) + numbered).first { it !in taken }
        return save(animation.copy(name = name))
    }

    suspend fun delete(id: String) = change {
        fileOf(id).delete()
        current.update { entries -> entries?.filterNot { it.id == id } }
    }

    private suspend fun <T> change(block: () -> T): T {
        all()
        return writing.withLock { withContext(Dispatchers.IO) { block() } }
    }

    private fun replacing(entry: AnimationEntry): AnimationEntry = entry.also {
        current.update { entries -> entries.orEmpty().filterNot { it.id == entry.id }.plus(entry).sortedByName() }
    }

    private suspend fun load(): List<AnimationEntry> = withContext(Dispatchers.IO) {
        directory.listFiles { file -> file.name.endsWith(AnimationFile.EXTENSION) }.orEmpty()
            .mapNotNull { file ->
                runCatching { json.decodeFromString<Animation>(AtomicFile(file).readFully().decodeToString()) }
                    .onFailure { Log.w(TAG, "Cannot read ${file.name}", it) }
                    .getOrNull()
                    ?.takeIf { it.device == codename && it.format <= Animation.FORMAT }
                    ?.let { AnimationEntry("$ID_PREFIX${file.name.removeSuffix(AnimationFile.EXTENSION)}", CATEGORY, it) }
            }
            .sortedByName()
    }

    private fun write(entry: AnimationEntry) {
        directory.mkdirs()
        val file = AtomicFile(fileOf(entry.id))
        val stream = file.startWrite()
        runCatching { stream.write(AnimationFile.encode(entry.animation)) }
            .onSuccess { file.finishWrite(stream) }
            .onFailure { file.failWrite(stream) }
            .getOrThrow()
    }

    private fun fileOf(id: String) = File(directory, id.removePrefix(ID_PREFIX) + AnimationFile.EXTENSION)

    private fun List<AnimationEntry>.sortedByName() = sortedBy { it.animation.name.lowercase() }

    companion object {
        const val CATEGORY = "user"

        private const val ID_PREFIX = "$CATEGORY:"
        private const val TAG = "GlyphUserAnimations"
    }
}
