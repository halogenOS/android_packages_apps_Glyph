/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.progress

import custom.glyph.settings.Setting

object ProgressSettings {
    val enabled = Setting.boolean("progress_enabled", true)
    val songPosition = Setting.boolean("progress_song_position", false)
    val excludedApps = Setting.stringSet("progress_excluded_apps", emptySet())
}
