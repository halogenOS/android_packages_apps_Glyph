/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Priority
import custom.glyph.features.R
import custom.glyph.frame.Frame
import custom.glyph.glyph
import custom.glyph.ui.apps.AppSwitchRow
import custom.glyph.ui.apps.InstalledApp
import custom.glyph.ui.apps.installedApps
import custom.glyph.ui.preview.AnimationPicker
import custom.glyph.ui.preview.GlyphGeometry
import custom.glyph.ui.preview.GlyphPreview
import custom.glyph.ui.preview.animationsIn
import custom.glyph.ui.settings.LinkRow
import custom.glyph.ui.settings.SettingsCard
import custom.glyph.ui.settings.SwitchRow
import custom.glyph.ui.settings.rememberSetting
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow

@Composable
fun NotificationsPage() {
    val enabled = rememberSetting(NotificationSettings.enabled)
    val animation = rememberSetting(NotificationSettings.animation)
    val soundLights = rememberSetting(NotificationSettings.soundLights)
    val muted = rememberSetting(NotificationSettings.mutedApps)
    val essential = rememberSetting(NotificationSettings.essentialApps)
    val animations by animationsIn(NotificationSettings.CATEGORY)
    val apps by installedApps()
    var pickingAnimation by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<InstalledApp?>(null) }

    SettingsCard {
        SwitchRow(stringResource(R.string.notifications_enabled), enabled.value, enabled.set)
        SwitchRow(
            title = stringResource(R.string.sound_lights),
            summary = stringResource(R.string.sound_lights_summary),
            checked = soundLights.value,
            onCheckedChange = soundLights.set,
            enabled = enabled.value,
        )
        LinkRow(
            title = stringResource(R.string.animation),
            summary = animations.firstOrNull { it.id == animation.value }?.animation?.name,
            onClick = { pickingAnimation = true },
        )
    }
    Text(
        text = stringResource(R.string.apps),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
    SettingsCard {
        apps.forEach { app ->
            AppSwitchRow(
                app = app,
                summary = if (app.packageName in essential.value) stringResource(R.string.essential) else null,
                checked = app.packageName !in muted.value,
                onCheckedChange = { lit -> muted.set(if (lit) muted.value - app.packageName else muted.value + app.packageName) },
                onClick = { editing = app },
            )
        }
    }

    if (pickingAnimation) {
        AlertDialog(
            onDismissRequest = { pickingAnimation = false },
            confirmButton = { TextButton(onClick = { pickingAnimation = false }) { Text(stringResource(android.R.string.ok)) } },
            title = { Text(stringResource(R.string.animation)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    AnimationPicker(animations, animation.value, onSelect = { animation.set(it.id) })
                }
            },
        )
    }
    editing?.let { app ->
        EssentialDialog(
            app = app,
            essential = app.packageName in essential.value,
            onEssential = { on -> essential.set(if (on) essential.value + app.packageName else essential.value - app.packageName) },
            onDismiss = { editing = null },
        )
    }
}

/** Whether [app] is essential, and which zone stays lit for it, chosen by tapping the drawing. */
@Composable
private fun EssentialDialog(app: InstalledApp, essential: Boolean, onEssential: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val glyph = LocalContext.current.glyph
    val device = glyph.device
    val geometry = remember(device) { GlyphGeometry(device) }
    val zone = rememberSetting(NotificationSettings.essentialZone(app.packageName, device))
    val leds = device.zones.firstOrNull { it.id == zone.value }?.leds.orEmpty()
    val drawn = Frame.dark(device.frame.size).with(leds, 1f)

    LaunchedEffect(leds, essential) {
        if (!essential) return@LaunchedEffect
        val glow = Frame.dark(device.frame.size).with(leds, NotificationSettings.ESSENTIAL_LEVEL)
        val frames = flow { emit(glow); awaitCancellation() }
        glyph.arbiter.show(GlyphRequest(PREVIEW_SOURCE, Priority.PREVIEW, frames, animated = false))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
        title = { Text(app.label) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SwitchRow(
                    title = stringResource(R.string.essential),
                    summary = stringResource(R.string.essential_summary),
                    checked = essential,
                    onCheckedChange = onEssential,
                )
                if (essential) {
                    Text(
                        text = stringResource(R.string.essential_zone_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    GlyphPreview(geometry, drawn, Modifier.height(260.dp), onZoneTap = { zone.set(it.id) })
                }
            }
        },
    )
}

private const val PREVIEW_SOURCE = "preview:essential"
