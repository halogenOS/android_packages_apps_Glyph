/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.progress

import android.content.Context
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Preemption
import custom.glyph.arbiter.Priority
import custom.glyph.feature.GlyphFeature
import custom.glyph.glyph
import custom.glyph.signal.songPosition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shows on the progress ring how far the most recently updated progress notification is, or
 * else, when asked for, how far the playing song is.
 */
class ProgressFeature(private val context: Context) : GlyphFeature {

    private val glyph = context.glyph

    override suspend fun run() {
        val ring = glyph.device.meters[PROGRESS_METER] ?: return
        glyph.settings[ProgressSettings.enabled].collectLatest { on -> if (on) followProgress(ring) }
    }

    /** One display at a time, which follows whatever is tracked until it finishes or nothing is. */
    private suspend fun followProgress(ring: List<Int>) {
        coroutineScope {
            val finished = MutableStateFlow(emptySet<String>())
            val tracked = combine(notificationProgress(finished), songProgress()) { notification, song -> notification ?: song }
                .stateIn(this, SharingStarted.Eagerly, null)
            var shown: Job? = null
            tracked.collect { progress ->
                if (progress == null || shown?.isActive == true) return@collect
                shown = launch {
                    val frames = progressFrames(glyph.device.frame.size, ring, tracked)
                    glyph.arbiter.show(GlyphRequest(SOURCE, Priority.PROGRESS, frames, Preemption.RESUME, animated = false))
                    tracked.value?.takeIf { it.completes && it.fraction >= 1f }?.let { done -> finished.update { it + done.key } }
                }
            }
        }
    }

    /** The newest progress notification from an app that may show it, leaving out finished ones. */
    private fun notificationProgress(finished: Flow<Set<String>>): Flow<TrackedProgress?> =
        combine(glyph.notifications.active, glyph.settings[ProgressSettings.excludedApps], finished) { active, excluded, done ->
            active.values
                .filter { it.packageName !in excluded }
                .mapNotNull { notification -> notification.progress?.let { notification to it } }
                .filterNot { (notification, progress) -> notification.key in done && progress.fraction >= 1f }
                .maxByOrNull { (notification, _) -> notification.postTime }
                ?.let { (notification, progress) -> TrackedProgress(notification.key, progress.fraction, progress.milestones) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun songProgress(): Flow<TrackedProgress?> =
        glyph.settings[ProgressSettings.songPosition].flatMapLatest { on ->
            if (!on) flowOf(null)
            else context.songPosition(glyph.notificationListener).map { position ->
                position?.let { TrackedProgress(SONG, it, completes = false) }
            }
        }

    private companion object {
        const val SOURCE = "progress"
        const val PROGRESS_METER = "progress"
        const val SONG = "song"
    }
}
