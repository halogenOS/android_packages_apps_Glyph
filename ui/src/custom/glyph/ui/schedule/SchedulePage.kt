/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.schedule

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import custom.glyph.glyph
import custom.glyph.schedule.QuietPeriod
import custom.glyph.ui.R
import custom.glyph.ui.settings.SettingsCard
import custom.glyph.ui.settings.SwitchRow
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/** The schedule switch, then every quiet period with its days and hours, and a button to add one. */
@Composable
fun SchedulePage() {
    val glyph = LocalContext.current.glyph
    val viewModel = viewModel { ScheduleViewModel(glyph) }
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    SettingsCard {
        SwitchRow(
            title = stringResource(R.string.schedule),
            summary = stringResource(R.string.schedule_summary),
            checked = schedule.enabled,
            onCheckedChange = viewModel::setEnabled,
        )
    }
    schedule.periods.forEach { period ->
        PeriodCard(
            period = period,
            editable = schedule.enabled,
            onEnabled = { viewModel.setPeriodEnabled(period.id, it) },
            onToggleDay = { viewModel.toggleDay(period.id, it) },
            onStart = { viewModel.setStart(period.id, it) },
            onEnd = { viewModel.setEnd(period.id, it) },
            onDelete = { viewModel.remove(period.id) },
        )
    }
    FilledTonalButton(onClick = viewModel::add, enabled = schedule.enabled, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Text(stringResource(R.string.schedule_add), modifier = Modifier.padding(start = 8.dp))
    }
}

/** One quiet period: its own switch, the days it applies to, its hours, and a way to delete it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodCard(
    period: QuietPeriod,
    editable: Boolean,
    onEnabled: (Boolean) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onStart: (LocalTime) -> Unit,
    onEnd: (LocalTime) -> Unit,
    onDelete: () -> Unit,
) {
    val locale = Locale.getDefault()
    val active = editable && period.enabled
    SettingsCard {
        SwitchRow(
            title = periodSummary(period),
            checked = period.enabled,
            onCheckedChange = onEnabled,
            enabled = editable,
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            weekDays(locale).forEach { day ->
                FilterChip(
                    selected = day in period.days,
                    onClick = { onToggleDay(day) },
                    enabled = active,
                    label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                )
            }
        }
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TimeButton(stringResource(R.string.schedule_start), period.start, active, onStart, Modifier.weight(1f))
            TimeButton(stringResource(R.string.schedule_end), period.end, active, onEnd, Modifier.weight(1f))
            IconButton(onClick = onDelete, enabled = editable) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.schedule_delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeButton(label: String, time: LocalTime, enabled: Boolean, onPick: (LocalTime) -> Unit, modifier: Modifier) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    var picking by remember { mutableStateOf(false) }
    TextButton(onClick = { picking = true }, enabled = enabled, modifier = modifier) {
        Text("$label  ${clockTime(time)}")
    }
    if (picking) {
        val state = rememberTimePickerState(time.hour, time.minute, is24Hour)
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    onPick(LocalTime.of(state.hour, state.minute))
                    picking = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(android.R.string.cancel)) } },
            text = { TimePicker(state) },
        )
    }
}
