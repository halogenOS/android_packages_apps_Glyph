/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.flip

import android.media.AudioManager
import custom.glyph.settings.Setting

object FlipSettings {
    /** No ringer mode is stored. */
    internal const val NONE = -1

    val enabled = Setting.boolean("flip_enabled", true)
    val ringerMode = Setting.int("flip_ringer_mode", AudioManager.RINGER_MODE_VIBRATE)

    /** The ringer mode from before the phone was laid face down, and the one flip set; [NONE] when none. */
    internal val ringerBefore = Setting.int("flip_ringer_before", NONE)
    internal val ringerApplied = Setting.int("flip_ringer_applied", NONE)
}
