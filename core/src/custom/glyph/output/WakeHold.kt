/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.output

import android.os.PowerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Keeps the CPU running while [active] is true, so what the Glyphs show keeps moving and ends
 * on time with the screen off.
 */
suspend fun PowerManager.stayAwakeWhile(active: Flow<Boolean>, tag: String) {
    val lock = newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag).apply { setReferenceCounted(false) }
    try {
        active.distinctUntilChanged().collect { if (it) lock.acquire() else lock.release() }
    } finally {
        lock.release()
    }
}
