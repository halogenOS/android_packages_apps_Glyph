/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.StatusBarNotification

/**
 * What the Glyphs need to know about one notification in the shade. [postTime] is when it was
 * last posted or updated; [progress] is set for determinate progress notifications.
 */
data class ActiveNotification(
    val key: String,
    val packageName: String,
    val channelId: String?,
    val category: String?,
    val ongoing: Boolean,
    val onlyAlertOnce: Boolean,
    val importance: Int,
    val passesDoNotDisturb: Boolean,
    val postTime: Long,
    val progress: NotificationProgress?,
    val sound: Uri?,
) {
    /** Whether Android lets it make itself noticed: not silent, and allowed through Do Not Disturb. */
    val audible: Boolean
        get() = importance >= NotificationManager.IMPORTANCE_DEFAULT && passesDoNotDisturb

    companion object {
        fun of(context: Context, notification: StatusBarNotification, ranking: Ranking) = ActiveNotification(
            key = notification.key,
            packageName = notification.packageName,
            channelId = notification.notification.channelId,
            category = notification.notification.category,
            ongoing = notification.isOngoing,
            onlyAlertOnce = notification.notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0,
            importance = ranking.importance,
            passesDoNotDisturb = ranking.matchesInterruptionFilter(),
            postTime = notification.postTime,
            progress = NotificationProgress.of(context, notification.notification),
            sound = ranking.channel?.sound ?: @Suppress("DEPRECATION") notification.notification.sound,
        )
    }
}
