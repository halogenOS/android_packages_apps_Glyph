/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.charging

import android.content.Context
import android.hardware.SensorManager
import android.os.BatteryManager
import custom.glyph.animation.meterFrames
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Preemption
import custom.glyph.arbiter.Priority
import custom.glyph.arbiter.collectWhenIdle
import custom.glyph.feature.GlyphFeature
import custom.glyph.glyph
import custom.glyph.signal.batteryLevel
import custom.glyph.signal.chargerConnections
import custom.glyph.signal.chargerPlug
import custom.glyph.signal.events
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * Shows the battery level on the battery meter when a charger is plugged in, and again when
 * the phone is moved while it lies flat on a cable charger.
 */
class ChargingFeature(private val context: Context) : GlyphFeature {

    private val glyph = context.glyph
    private val sensors = context.getSystemService(SensorManager::class.java)

    override suspend fun run() {
        val meter = glyph.device.meters[BATTERY_METER] ?: return
        glyph.settings[ChargingSettings.batteryLevel].collectLatest { on -> if (on) showLevelWhileCharging(meter) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun showLevelWhileCharging(meter: List<Int>) {
        val flatMotionSensor = glyph.device.sensors[FLAT_MOTION]
        val motionsOnCable = context.chargerPlug()
            .map { it and CABLE_PLUGS != 0 }
            .distinctUntilChanged()
            .flatMapLatest { onCable -> if (onCable && flatMotionSensor != null) flatMotions(flatMotionSensor) else emptyFlow() }
        merge(context.chargerConnections(), motionsOnCable).collectWhenIdle {
            val frames = meterFrames(glyph.device.frame.size, meter, context.batteryLevel, HOLD_MS)
            glyph.arbiter.show(GlyphRequest(SOURCE, Priority.CHARGING, frames, Preemption.DROP))
        }
    }

    /** Each time the phone is moved while lying flat, then a pause before the sensor listens again. */
    private fun flatMotions(sensor: String): Flow<Unit> = flow {
        while (true) {
            sensors.events(sensor).firstOrNull { it.firstOrNull() == MOVED } ?: return@flow
            emit(Unit)
            delay(MOTION_PAUSE_MS)
        }
    }

    private companion object {
        const val SOURCE = "charging"
        const val BATTERY_METER = "battery"
        const val FLAT_MOTION = "flatMotion"
        const val CABLE_PLUGS = BatteryManager.BATTERY_PLUGGED_AC or BatteryManager.BATTERY_PLUGGED_USB
        const val MOVED = 1f
        const val HOLD_MS = 1_000L
        const val MOTION_PAUSE_MS = 3_000L
    }
}
