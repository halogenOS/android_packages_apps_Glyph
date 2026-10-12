/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.charging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import custom.glyph.features.R
import custom.glyph.glyph
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeaturePage
import custom.glyph.ui.settings.LinkRow
import custom.glyph.ui.settings.SettingsCard
import custom.glyph.ui.settings.SwitchRow
import custom.glyph.ui.settings.rememberSetting

/**
 * The battery level while charging, and reverse charging where the phone has it. With both,
 * the main screen shows a row that opens them; with only the battery level, its switch.
 */
object ChargingEntry : FeatureEntry {
    override val id = "charging"

    @Composable
    override fun Row(openPage: () -> Unit) {
        val batteryLevel = rememberSetting(ChargingSettings.batteryLevel)
        if (!hasReverseCharging()) {
            BatteryLevelSwitch()
            return
        }
        val reverseCharging = rememberSetting(ChargingSettings.reverseCharging)
        val on = listOfNotNull(
            stringResource(R.string.charging_battery_level).takeIf { batteryLevel.value },
            stringResource(R.string.charging_reverse).takeIf { reverseCharging.value },
        )
        LinkRow(stringResource(R.string.charging), on.joinToString().ifEmpty { stringResource(R.string.off) }, openPage)
    }

    override val page = FeaturePage(R.string.charging) {
        SettingsCard {
            BatteryLevelSwitch()
            val reverseCharging = rememberSetting(ChargingSettings.reverseCharging)
            SwitchRow(
                title = stringResource(R.string.charging_reverse),
                summary = stringResource(R.string.charging_reverse_summary),
                checked = reverseCharging.value,
                onCheckedChange = reverseCharging.set,
            )
        }
    }

    @Composable
    private fun hasReverseCharging(): Boolean {
        val device = LocalContext.current.glyph.device
        return remember(device) { ReverseChargingFeature.isSupported(device) }
    }

    @Composable
    private fun BatteryLevelSwitch() {
        val batteryLevel = rememberSetting(ChargingSettings.batteryLevel)
        SwitchRow(
            title = stringResource(R.string.charging_battery_level),
            summary = stringResource(R.string.charging_battery_level_summary),
            checked = batteryLevel.value,
            onCheckedChange = batteryLevel.set,
        )
    }
}
