/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.signal

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * The values of every event of the sensor with this string type, for as long as the flow is
 * collected. Sensors are found by name because vendors reuse the numeric types beyond the
 * Android ones. A device without the sensor yields an empty flow.
 */
fun SensorManager.events(stringType: String): Flow<FloatArray> {
    val sensor = getSensorList(Sensor.TYPE_ALL).firstOrNull { it.stringType == stringType } ?: return emptyFlow()
    return callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values.copyOf())
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { unregisterListener(listener) }
    }
}
