/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.notification

import android.app.Notification
import android.content.Context

/**
 * How far a progress notification has come, from 0 to 1, and the [milestones] along the way
 * where one of its segments ends or one of its points sits, as fractions of the whole.
 */
data class NotificationProgress(val fraction: Float, val milestones: List<Float>) {

    companion object {
        /** The progress of a determinate [Notification.ProgressStyle] notification, or null for any other. */
        fun of(context: Context, notification: Notification): NotificationProgress? {
            if (notification.extras.getString(Notification.EXTRA_TEMPLATE) != Notification.ProgressStyle::class.java.name) return null
            val style = Notification.Builder.recoverBuilder(context, notification).style as? Notification.ProgressStyle ?: return null
            if (style.isProgressIndeterminate) return null
            val max = style.progressMax.toFloat()
            val segmentEnds = style.progressSegments.runningFold(0) { end, segment -> end + segment.length }.drop(1).dropLast(1)
            val points = style.progressPoints.map { it.position }
            return NotificationProgress(
                fraction = (style.progress / max).coerceIn(0f, 1f),
                milestones = (segmentEnds + points).map { it / max }.filter { it in 0f..1f }.distinct().sorted(),
            )
        }
    }
}
