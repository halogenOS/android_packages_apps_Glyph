/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package com.nothing.thirdparty

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import custom.glyph.sdk.ISdkHost

/**
 * The connection to the Glyph app. Calls made before it is up wait and go out in order once it
 * is; calls made while the Glyph app restarts are lost with the sessions it held.
 */
class HostLink(private val context: Context) : ServiceConnection {

    private var host: ISdkHost? = null
    private val waiting = ArrayDeque<(ISdkHost) -> Unit>()

    fun connect() {
        context.bindService(Intent().setComponent(HOST), this, Context.BIND_AUTO_CREATE)
    }

    fun disconnect() {
        context.unbindService(this)
        synchronized(this) {
            host = null
            waiting.clear()
        }
    }

    fun send(call: (ISdkHost) -> Unit) = synchronized(this) {
        host?.deliver(call) ?: waiting.addLast(call)
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) = synchronized(this) {
        val connected = ISdkHost.Stub.asInterface(binder).also { host = it }
        while (waiting.isNotEmpty()) connected.deliver(waiting.removeFirst())
    }

    override fun onServiceDisconnected(name: ComponentName) = synchronized(this) {
        host = null
    }

    private fun ISdkHost.deliver(call: (ISdkHost) -> Unit) {
        try {
            call(this)
        } catch (e: RemoteException) {
            Log.w(TAG, "Glyph app unreachable", e)
        }
    }

    private companion object {
        const val TAG = "GlyphSdkBridge"
        val HOST = ComponentName("custom.glyph", "custom.glyph.features.sdk.SdkHostService")
    }
}
