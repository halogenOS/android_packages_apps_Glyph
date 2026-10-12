/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.signal

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Emits each time a charger is plugged in, never for one that was already connected. */
fun Context.chargerConnections(): Flow<Unit> = broadcasts(Intent.ACTION_POWER_CONNECTED).map { }

/** The connected charger as [BatteryManager]'s `BATTERY_PLUGGED_*` flags, 0 without one, starting with the current one. */
fun Context.chargerPlug(): Flow<Int> =
    broadcasts(Intent.ACTION_BATTERY_CHANGED)
        .map { it.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) }
        .distinctUntilChanged()

/** The battery charge as a fraction from 0 to 1. */
val Context.batteryLevel: Float
    get() = getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100f
