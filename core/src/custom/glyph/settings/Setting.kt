/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.settings

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/** A stored value with the value it has until someone sets it. */
class Setting<T>(val key: Preferences.Key<T>, val default: T) {

    companion object {
        fun boolean(name: String, default: Boolean) = Setting(booleanPreferencesKey(name), default)
        fun float(name: String, default: Float) = Setting(floatPreferencesKey(name), default)
        fun int(name: String, default: Int) = Setting(intPreferencesKey(name), default)
        fun string(name: String, default: String) = Setting(stringPreferencesKey(name), default)
        fun stringSet(name: String, default: Set<String>) = Setting(stringSetPreferencesKey(name), default)
    }
}
