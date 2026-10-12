/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import android.app.AlarmManager
import android.content.Context
import custom.glyph.feature.GlyphFeature

/** Wakes the phone when the running Glyph Timer is up, even in deep sleep, so it rings on time. */
class TimerExpiry(private val context: Context) : GlyphFeature {

    private val alarms = context.getSystemService(AlarmManager::class.java)

    override suspend fun run() {
        context.glyphTimer.state.collect { state ->
            val expire = TimerAction.EXPIRE.pendingIntent(context)
            if (state is TimerState.Running) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, state.endsAt, expire)
            } else {
                alarms.cancel(expire)
            }
        }
    }
}
