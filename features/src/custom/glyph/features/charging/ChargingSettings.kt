/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.charging

import custom.glyph.settings.Setting

object ChargingSettings {
    val batteryLevel = Setting.boolean("charging_battery_level", true)
    val reverseCharging = Setting.boolean("charging_reverse", true)
}
