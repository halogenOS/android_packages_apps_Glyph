/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.arbiter

import android.util.Log
import custom.glyph.output.GlyphLights
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Decides which request owns the Glyphs: the most important one the [rules] allow, the newest
 * among equals. Requests that drop on preemption end as soon as they are not the one shown.
 */
class GlyphArbiter(private val lights: GlyphLights, rules: Flow<QuietRules>, scope: CoroutineScope) {

    private class Entry(val request: GlyphRequest) {
        val done = CompletableDeferred<Unit>()
    }

    private val entries = MutableStateFlow<List<Entry>>(emptyList())

    private val current = MutableStateFlow<GlyphRequest?>(null)

    /** The request being shown, or null while the Glyphs are dark. */
    val showing: StateFlow<GlyphRequest?> = current.asStateFlow()

    /** Shows [request] whenever it wins and returns once it has ended. Cancelling the caller withdraws it. */
    suspend fun show(request: GlyphRequest) {
        val entry = Entry(request)
        entries.update { it + entry }
        try {
            entry.done.await()
        } finally {
            entries.update { it - entry }
        }
    }

    init {
        scope.launch {
            combine(entries, rules) { all, rules -> winnerOf(all, rules) }
                .distinctUntilChanged()
                .collectLatest(::present)
        }
    }

    private fun winnerOf(all: List<Entry>, rules: QuietRules): Entry? {
        val allowed = all.filter { rules.allows(it.request) }
        val top = allowed.maxOfOrNull { it.request.priority }
        val winner = allowed.lastOrNull { it.request.priority == top }
        all.filter { it !== winner && it.request.preemption == Preemption.DROP }
            .filter { it.done.complete(Unit) }
            .forEach { Log.d(TAG, "Dropped ${it.request.describe()} for ${winner?.request?.describe()}") }
        return winner
    }

    private suspend fun present(winner: Entry?) {
        current.value = winner?.request
        if (winner == null) {
            lights.clear()
            return
        }
        Log.d(TAG, "Showing ${winner.request.describe()}")
        winner.request.frames.collect { lights.show(it, winner.request.fullBrightness) }
        Log.d(TAG, "Finished ${winner.request.describe()}")
        winner.done.complete(Unit)
    }

    private fun GlyphRequest.describe() = "$source ($priority)"

    private companion object {
        const val TAG = "GlyphArbiter"
    }
}
