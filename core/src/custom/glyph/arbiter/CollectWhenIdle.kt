/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.arbiter

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Runs [action] for a value only when the one for an earlier value has finished; values that
 * arrive in between are dropped, so a burst of triggers shows one animation instead of a queue.
 */
suspend fun <T> Flow<T>.collectWhenIdle(action: suspend (T) -> Unit) = coroutineScope {
    var running: Job? = null
    collect { value ->
        if (running?.isActive != true) running = launch { action(value) }
    }
}
