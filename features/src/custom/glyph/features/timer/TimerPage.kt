/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import custom.glyph.features.R
import custom.glyph.ui.settings.LinkRow
import custom.glyph.ui.settings.SettingsCard
import custom.glyph.ui.settings.SwitchRow
import custom.glyph.ui.settings.rememberSetting

private const val MAX_MINUTES = 99
private const val MAX_SECONDS = 59

@Composable
fun TimerPage() {
    val seconds = rememberSetting(TimerSettings.durationSeconds)
    val muteAlarm = rememberSetting(TimerSettings.muteAlarm)
    val clockTimers = rememberSetting(TimerSettings.clockTimers)
    var choosing by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.timer_how_to),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
    SettingsCard {
        LinkRow(stringResource(R.string.timer_duration), timerText(seconds.value * 1000L), onClick = { choosing = true })
        SwitchRow(
            title = stringResource(R.string.timer_mute_alarm),
            summary = stringResource(R.string.timer_mute_alarm_summary),
            checked = muteAlarm.value,
            onCheckedChange = muteAlarm.set,
        )
        SwitchRow(
            title = stringResource(R.string.timer_clock_timers),
            summary = stringResource(R.string.timer_clock_timers_summary),
            checked = clockTimers.value,
            onCheckedChange = clockTimers.set,
        )
    }
    if (choosing) {
        DurationDialog(seconds.value, onChoose = { seconds.set(it); choosing = false }, onDismiss = { choosing = false })
    }
}

@Composable
private fun DurationDialog(seconds: Int, onChoose: (Int) -> Unit, onDismiss: () -> Unit) {
    var minutesText by remember { mutableStateOf((seconds / 60).toString()) }
    var secondsText by remember { mutableStateOf((seconds % 60).toString()) }
    val total = (minutesText.toIntOrNull() ?: 0) * 60 + (secondsText.toIntOrNull() ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.timer_duration)) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(minutesText, { minutesText = it }, R.string.timer_minutes, MAX_MINUTES, Modifier.weight(1f))
                NumberField(secondsText, { secondsText = it }, R.string.timer_seconds, MAX_SECONDS, Modifier.weight(1f))
            }
        },
        confirmButton = {
            TextButton(onClick = { onChoose(total) }, enabled = total > 0) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

/** A field for a whole number from 0 to [max], which ignores anything else typed into it. */
@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, @StringRes label: Int, max: Int, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { typed -> if (typed.length <= max.toString().length && typed.all(Char::isDigit) && (typed.toIntOrNull() ?: 0) <= max) onValueChange(typed) },
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
