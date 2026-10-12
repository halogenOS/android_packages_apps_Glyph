/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.signal

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

private const val ALARM_TAG = "custom.glyph:clock"

/** Emits whenever the wall clock or the time zone is changed by hand or by the network. */
fun Context.clockChanges(): Flow<Unit> =
    broadcasts(Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED).map { }

/**
 * Emits once at [time] and completes. The alarm does not wake the device: anything that
 * lights the Glyphs while it sleeps re-evaluates the clock on its own.
 */
fun Context.alarmAt(time: LocalDateTime): Flow<Unit> = callbackFlow {
    val alarms = getSystemService(AlarmManager::class.java)
    val listener = AlarmManager.OnAlarmListener {
        trySend(Unit)
        close()
    }
    val at = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    alarms.setExact(AlarmManager.RTC, at, ALARM_TAG, listener, null)
    awaitClose { alarms.cancel(listener) }
}
