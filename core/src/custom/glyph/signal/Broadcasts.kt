/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.signal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Every broadcast with one of these actions, for as long as the flow is collected. A sticky
 * broadcast arrives once right away with its last value, unless [skipSticky] asks for new
 * ones only. Broadcasts from other apps arrive only when [fromApps] allows them.
 */
fun Context.broadcasts(vararg actions: String, skipSticky: Boolean = false, fromApps: Boolean = false): Flow<Intent> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (skipSticky && isInitialStickyBroadcast) return
            trySend(intent)
        }
    }
    val filter = IntentFilter().apply { actions.forEach(::addAction) }
    registerReceiver(receiver, filter, if (fromApps) Context.RECEIVER_EXPORTED else Context.RECEIVER_NOT_EXPORTED)
    awaitClose { unregisterReceiver(receiver) }
}
