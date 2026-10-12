/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** A Quick Settings tile that shows each of its [states] while Quick Settings is open. */
abstract class StateTile<T> : TileService() {

    /** Lives as long as the tile service; clicks launch their work here. */
    protected val scope = MainScope()

    private var listening: Job? = null

    protected abstract fun states(): Flow<T>

    protected abstract fun Tile.show(state: T)

    override fun onStartListening() {
        listening = scope.launch {
            states().collect { state -> qsTile?.run { show(state); updateTile() } }
        }
    }

    override fun onStopListening() {
        listening?.cancel()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
