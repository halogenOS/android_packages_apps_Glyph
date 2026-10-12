/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** All settings of the app in one store; every reader sees each change as it happens. */
class GlyphSettings(context: Context) {

    private val store = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(NAME) }

    operator fun <T> get(setting: Setting<T>): Flow<T> =
        store.data.map { it[setting.key] ?: setting.default }.distinctUntilChanged()

    suspend fun <T> set(setting: Setting<T>, value: T) {
        store.edit { it[setting.key] = value }
    }

    private companion object {
        const val NAME = "glyph"
    }
}
