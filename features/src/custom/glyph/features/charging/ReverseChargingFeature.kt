/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.charging

import android.content.Context
import custom.glyph.animation.frames
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Preemption
import custom.glyph.arbiter.Priority
import custom.glyph.device.GlyphDevice
import custom.glyph.feature.GlyphFeature
import custom.glyph.glyph
import custom.glyph.signal.fileWrites
import custom.glyph.signal.readNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart

/**
 * Plays a short light each time a device starts charging from the back. The driver announces
 * neither the switch nor the status, so like stock the status is polled, and only while
 * reverse charging is switched on.
 */
class ReverseChargingFeature(context: Context) : GlyphFeature {

    private val glyph = context.glyph

    override suspend fun run() {
        val switch = glyph.device.nodes[SWITCH_NODE] ?: return
        val status = glyph.device.nodes[STATUS_NODE] ?: return
        val animation = glyph.library.entry(ANIMATION)?.animation ?: return
        glyph.settings[ChargingSettings.reverseCharging].collectLatest { on ->
            if (!on) return@collectLatest
            deviceOnBack(switch, status).filter { it }.collect {
                glyph.arbiter.show(GlyphRequest(SOURCE, Priority.CHARGING, animation.frames(), Preemption.DROP))
            }
        }
    }

    /** Whether a device charges from the back, checked every moment reverse charging is switched on. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun deviceOnBack(switch: String, status: String): Flow<Boolean> =
        fileWrites(switch)
            .onStart { emit(Unit) }
            .flatMapLatest {
                flow {
                    while (readNode(switch) == SWITCHED_ON) {
                        emit(readNode(status)?.removePrefix(HEX_PREFIX)?.toIntOrNull(16) == CHARGING)
                        delay(POLL_MS)
                    }
                    emit(false)
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    companion object {
        /** Whether [device] reports reverse charging at all. */
        fun isSupported(device: GlyphDevice) = SWITCH_NODE in device.nodes && STATUS_NODE in device.nodes

        private const val SOURCE = "reverse_charging"
        private const val ANIMATION = "system/reverse_charging"
        private const val SWITCH_NODE = "reverseChargingSwitch"
        private const val STATUS_NODE = "reverseChargingStatus"
        private const val SWITCHED_ON = "1"
        private const val HEX_PREFIX = "0x"
        private const val CHARGING = 1
        private const val POLL_MS = 500L
    }
}
