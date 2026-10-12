/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.calls

import custom.glyph.settings.Setting

object CallSettings {
    const val CATEGORY = "call"

    val enabled = Setting.boolean("calls_enabled", true)
    val animation = Setting.string("calls_animation", "$CATEGORY/beetle_custom")

    /** Whether a Glyph Composer ringtone plays its own lights instead of [animation]. */
    val ringtoneLights = Setting.boolean("calls_ringtone_lights", true)
}
