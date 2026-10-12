/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.notifications

import custom.glyph.device.GlyphDevice
import custom.glyph.settings.Setting

object NotificationSettings {
    const val CATEGORY = "notification"

    /** How bright an essential zone glows. */
    const val ESSENTIAL_LEVEL = 0.6f

    val enabled = Setting.boolean("notifications_enabled", true)
    val animation = Setting.string("notifications_animation", "$CATEGORY/beak_custom")

    /** Whether a notification whose sound is a Glyph Composer ringtone plays that ringtone's lights. */
    val soundLights = Setting.boolean("notifications_sound_lights", true)
    val mutedApps = Setting.stringSet("notifications_muted_apps", emptySet())
    val essentialApps = Setting.stringSet("notifications_essential_apps", emptySet())

    /** The zone that stays lit for an essential app, by default the device's own choice. */
    fun essentialZone(packageName: String, device: GlyphDevice) =
        Setting.string("notifications_essential_zone:$packageName", device.defaultEssentialZone)

    private val GlyphDevice.defaultEssentialZone: String
        get() = zoneRoles[ESSENTIAL_ROLE] ?: zones.first().id

    private const val ESSENTIAL_ROLE = "essential"
}
