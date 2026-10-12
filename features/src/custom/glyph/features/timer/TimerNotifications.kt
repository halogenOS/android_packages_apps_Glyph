/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import custom.glyph.feature.GlyphFeature
import custom.glyph.features.R
import custom.glyph.ui.GlyphActivity

/**
 * Keeps a notification for the Glyph Timer while it counts down, pauses or rings, with the
 * buttons that fit. Tapping it opens the timer's page.
 */
class TimerNotifications(private val context: Context) : GlyphFeature {

    private val notifications = context.getSystemService(NotificationManager::class.java)

    override suspend fun run() {
        notifications.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_COUNTDOWN, context.getString(R.string.timer_channel_countdown), NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CHANNEL_ALARM, context.getString(R.string.timer_channel_alarm), NotificationManager.IMPORTANCE_HIGH)
                    .apply { setSound(null, null) },
            )
        )
        context.glyphTimer.state.collect { state ->
            notificationFor(state)?.let { notifications.notify(ID, it) } ?: notifications.cancel(ID)
        }
    }

    private fun notificationFor(state: TimerState): Notification? = when (state) {
        is TimerState.Running -> countdown()
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(System.currentTimeMillis() + state.remaining())
            .addAction(TimerAction.PAUSE, R.string.timer_pause)
            .addAction(TimerAction.STOP, R.string.timer_stop)
            .build()
        is TimerState.Paused -> countdown()
            .setContentText(context.getString(R.string.timer_paused_left, timerText(state.remaining)))
            .addAction(TimerAction.RESUME, R.string.timer_resume)
            .addAction(TimerAction.STOP, R.string.timer_stop)
            .build()
        TimerState.Ringing -> builder(CHANNEL_ALARM)
            .setContentText(context.getString(R.string.timer_times_up))
            .setCategory(Notification.CATEGORY_ALARM)
            .addAction(TimerAction.STOP, R.string.timer_stop)
            .build()
        TimerState.Idle, is TimerState.Ready -> null
    }

    private fun countdown() = builder(CHANNEL_COUNTDOWN).setCategory(Notification.CATEGORY_STOPWATCH)

    private fun builder(channel: String) = Notification.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_glyph_timer)
        .setContentTitle(context.getString(R.string.timer))
        .setContentIntent(
            PendingIntent.getActivity(context, 0, GlyphActivity.pageIntent(context, TimerEntry.id), PendingIntent.FLAG_IMMUTABLE)
        )
        .setOngoing(true)
        .setVisibility(Notification.VISIBILITY_PUBLIC)

    private fun Notification.Builder.addAction(action: TimerAction, title: Int) = addAction(
        Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_glyph_timer), context.getString(title), action.pendingIntent(context)).build()
    )

    private companion object {
        const val ID = 1
        const val CHANNEL_COUNTDOWN = "timer_countdown"
        const val CHANNEL_ALARM = "timer_alarm"
    }
}
