/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.notification

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The notifications in the shade as the listener reports them, for features to follow. */
class NotificationHub {

    private val shade = MutableStateFlow<Map<String, ActiveNotification>>(emptyMap())
    private val posts = MutableSharedFlow<ActiveNotification>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Every notification in the shade by key. */
    val active: StateFlow<Map<String, ActiveNotification>> = shade.asStateFlow()

    /**
     * Notifications as they alert: newly posted ones, and updates that are allowed to alert
     * again. Whether one may be heard at all is left to [ActiveNotification.audible].
     */
    val alerts: SharedFlow<ActiveNotification> = posts.asSharedFlow()

    internal fun connected(notifications: List<ActiveNotification>) {
        shade.value = notifications.associateBy { it.key }
    }

    internal fun posted(notification: ActiveNotification) {
        val update = notification.key in shade.value
        shade.update { it + (notification.key to notification) }
        if (!update || !notification.onlyAlertOnce) posts.tryEmit(notification)
    }

    internal fun removed(key: String) = shade.update { it - key }

    internal fun disconnected() {
        shade.value = emptyMap()
    }
}
