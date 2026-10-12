/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** A step of the Glyph Timer that a notification button or an alarm can take later. */
enum class TimerAction(val apply: (GlyphTimer) -> Unit) {
    PAUSE(GlyphTimer::pause),
    RESUME(GlyphTimer::resume),
    STOP(GlyphTimer::stop),
    EXPIRE(GlyphTimer::expire);

    fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        ordinal,
        Intent(context, TimerActionReceiver::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerAction.entries.firstOrNull { it.name == intent.action }?.apply?.invoke(context.glyphTimer)
    }
}
