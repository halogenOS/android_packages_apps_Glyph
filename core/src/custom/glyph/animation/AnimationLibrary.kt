/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.animation

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

/** An animation together with where it comes from. [id] is stable and unique within the library. */
data class AnimationEntry(val id: String, val category: String, val animation: Animation)

/**
 * The animations shipped with the device variant, from `assets/animations/<category>/<name>.glyph`,
 * the ones the user made in [user], and the user's Glyph Composer ringtones from [compositions].
 */
class AnimationLibrary(
    private val assets: AssetManager,
    private val codename: String,
    val user: UserAnimations,
    val compositions: CompositionLibrary,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val entries = lazy(::load)

    /** Every animation, read once and kept: the assets never change while the app runs. */
    suspend fun builtIn(): List<AnimationEntry> = withContext(Dispatchers.IO) { entries.value }

    suspend fun inCategory(category: String): List<AnimationEntry> = builtIn().filter { it.category == category }

    /** What one can pick for [category]: its own animations, then the user's, then the compositions. */
    suspend fun choices(category: String): List<AnimationEntry> = inCategory(category) + user.all() + compositions.all()

    suspend fun entry(id: String): AnimationEntry? = builtIn().firstOrNull { it.id == id } ?: user.entry(id) ?: compositions.entry(id)

    private fun load(): List<AnimationEntry> =
        assets.list(ROOT).orEmpty().sorted().flatMap { category ->
            assets.list("$ROOT/$category").orEmpty()
                .filter { it.endsWith(EXTENSION) }
                .map { file -> AnimationEntry("$category/${file.removeSuffix(EXTENSION)}", category, read("$ROOT/$category/$file")) }
                .filter { it.animation.device == codename && it.animation.format <= Animation.FORMAT }
                .sortedBy { it.animation.name.lowercase() }
        }

    private fun read(path: String): Animation = assets.open(path).use(json::decodeFromStream)

    private companion object {
        const val ROOT = "animations"
        const val EXTENSION = ".glyph"
    }
}
