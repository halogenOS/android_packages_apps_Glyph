/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.signal

import android.os.FileObserver
import java.io.File
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Each time something writes to the file at [path], for as long as the flow is collected. A
 * driver changing the value on its own writes nothing, so this only sees other processes.
 */
fun fileWrites(path: String): Flow<Unit> = callbackFlow {
    val observer = object : FileObserver(File(path), MODIFY or CLOSE_WRITE) {
        override fun onEvent(event: Int, file: String?) {
            trySend(Unit)
        }
    }
    observer.startWatching()
    awaitClose { observer.stopWatching() }
}

/** The file's content without surrounding whitespace, or null when it cannot be read. */
fun readNode(path: String): String? = runCatching { File(path).readText().trim() }.getOrNull()
