/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.flip

import android.content.Context
import android.hardware.SensorManager
import custom.glyph.glyph
import custom.glyph.signal.events
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

private const val SCREEN_FACING = "screenFacing"
private const val FACE_DOWN = 2f

/** Whether the phone lies face down, from its screen facing sensor; nothing on a device without one. */
fun Context.faceDown(): Flow<Boolean> {
    val sensor = glyph.device.sensors[SCREEN_FACING] ?: return emptyFlow()
    return getSystemService(SensorManager::class.java).events(sensor)
        .map { it.firstOrNull() == FACE_DOWN }
        .distinctUntilChanged()
}
