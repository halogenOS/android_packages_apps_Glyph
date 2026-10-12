/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.signal

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** The uid of each app whose last process ends, for as long as the flow is collected. */
fun Context.appsGone(): Flow<Int> = callbackFlow {
    val activities = getSystemService(ActivityManager::class.java)
    val listener = ActivityManager.OnUidImportanceListener { uid, importance ->
        if (importance == RunningAppProcessInfo.IMPORTANCE_GONE) trySend(uid)
    }
    activities.addOnUidImportanceListener(listener, RunningAppProcessInfo.IMPORTANCE_CACHED)
    awaitClose { activities.removeOnUidImportanceListener(listener) }
}
