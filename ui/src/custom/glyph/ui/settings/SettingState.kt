/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import custom.glyph.glyph
import custom.glyph.settings.Setting
import kotlinx.coroutines.launch

/** The current value of a setting on screen, and how to change it. */
class SettingState<T>(val value: T, val set: (T) -> Unit)

@Composable
fun <T> rememberSetting(setting: Setting<T>): SettingState<T> {
    val settings = LocalContext.current.glyph.settings
    val value by remember(setting.key) { settings[setting] }.collectAsStateWithLifecycle(setting.default)
    val scope = rememberCoroutineScope()
    return SettingState(value) { scope.launch { settings.set(setting, it) } }
}
