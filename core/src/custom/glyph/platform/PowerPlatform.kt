/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.platform

import android.os.PowerManager
import android.os.SystemClock

private val goToSleep by lazy { HiddenMembers.method(PowerManager::class, "goToSleep", Long::class.javaPrimitiveType!!) }

/** Turns the screen off now, as the power button does. */
fun PowerManager.sleepNow() {
    goToSleep.invoke(this, SystemClock.uptimeMillis())
}
