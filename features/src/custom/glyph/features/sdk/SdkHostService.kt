/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.sdk

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import custom.glyph.sdk.ISdkHost

/** Takes the Glyph Developer Kit calls the bridge app passes on. */
class SdkHostService : Service() {

    private val binder = object : ISdkHost.Stub() {
        override fun register(uid: Int, packageName: String, model: String) {
            Log.d(TAG, "$packageName registered for $model")
            sdkSessions.register(uid, model)
        }

        override fun open(uid: Int, packageName: String) {
            Log.d(TAG, "$packageName opened a session")
            sdkSessions.open(uid, packageName)
        }

        override fun show(uid: Int, levels: IntArray) = sdkSessions.show(uid, levels)

        override fun close(uid: Int) {
            Log.d(TAG, "$uid closed its session")
            sdkSessions.close(uid)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    private companion object {
        const val TAG = "GlyphSdk"
    }
}
