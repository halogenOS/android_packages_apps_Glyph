/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.microphone

import custom.glyph.settings.Setting

object MicrophoneSettings {
    val enabled = Setting.boolean("microphone_light", true)
    val mode = Setting.string("microphone_light_mode", MicrophoneLightMode.BLINK.name)
    val apps = Setting.stringSet("microphone_light_apps", emptySet())
}
