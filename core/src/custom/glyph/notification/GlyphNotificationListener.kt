/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import custom.glyph.glyph

/** Hands what the system reports about notifications to the process's [NotificationHub]. */
class GlyphNotificationListener : NotificationListenerService() {

    private val hub get() = glyph.notifications

    override fun onListenerConnected() {
        val ranking = currentRanking
        hub.connected(activeNotifications.orEmpty().mapNotNull { notification -> notification.withRanking(ranking) })
    }

    override fun onListenerDisconnected() = hub.disconnected()

    override fun onNotificationPosted(notification: StatusBarNotification, rankingMap: RankingMap) {
        notification.withRanking(rankingMap)?.let(hub::posted)
    }

    override fun onNotificationRemoved(notification: StatusBarNotification, rankingMap: RankingMap) {
        hub.removed(notification.key)
    }

    private fun StatusBarNotification.withRanking(rankingMap: RankingMap): ActiveNotification? {
        val ranking = Ranking()
        return if (rankingMap.getRanking(key, ranking)) ActiveNotification.of(this@GlyphNotificationListener, this, ranking) else null
    }
}
