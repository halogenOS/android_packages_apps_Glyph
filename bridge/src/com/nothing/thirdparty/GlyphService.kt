/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package com.nothing.thirdparty

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * The service Glyph Developer Kit apps bind to. It tells the Glyph app which app called and
 * what it asked for; the Glyph app decides what the LEDs show. Glyph Matrix calls have no
 * matrix to drive here and are accepted without effect.
 */
class GlyphService : Service() {

    private val host = HostLink(this)

    private val binder = object : IGlyphService.Stub() {
        override fun setFrameColors(colors: IntArray?) {
            val uid = Binder.getCallingUid()
            colors?.let { levels -> host.send { it.show(uid, levels) } }
        }

        override fun openSession() {
            val (uid, packageName) = caller()
            host.send { it.open(uid, packageName) }
        }

        override fun closeSession() {
            val uid = Binder.getCallingUid()
            host.send { it.close(uid) }
        }

        override fun register(model: String?) = registered(model)

        override fun registerSDK(apiKey: String?, model: String?) = registered(model)

        override fun registerMatrixSDK(apiKey: String?) = true
        override fun setMatrixColors(colors: IntArray?) = Unit
        override fun setGlyphMatrixTimeout(enabled: Boolean) = Unit
        override fun setAppMatrixColors(colors: IntArray?) = Unit
        override fun closeAppMatrix() = Unit
    }

    private fun registered(model: String?): Boolean {
        model ?: return false
        val (uid, packageName) = caller()
        host.send { it.register(uid, packageName, model) }
        return true
    }

    private fun caller(): Pair<Int, String> {
        val uid = Binder.getCallingUid()
        return uid to (packageManager.getNameForUid(uid) ?: uid.toString())
    }

    override fun onCreate() {
        super.onCreate()
        host.connect()
    }

    override fun onDestroy() {
        host.disconnect()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder = binder
}
