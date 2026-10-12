/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.schedule

import android.content.Context
import custom.glyph.signal.alarmAt
import custom.glyph.signal.clockChanges
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

/** Whether [schedule] is active, re-evaluated at each of its boundaries and whenever the clock is changed. */
@OptIn(ExperimentalCoroutinesApi::class)
fun Context.scheduleActivity(schedule: Schedule): Flow<Boolean> =
    clockChanges().onStart { emit(Unit) }.flatMapLatest {
        flow {
            while (true) {
                val now = LocalDateTime.now()
                emit(schedule.isActive(now))
                val next = schedule.nextChange(now) ?: break
                alarmAt(next).first()
            }
        }
    }.distinctUntilChanged()
