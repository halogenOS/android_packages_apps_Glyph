/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.hardware.SensorManager
import android.os.PowerManager
import custom.glyph.animation.AnimationLibrary
import custom.glyph.animation.CompositionLibrary
import custom.glyph.animation.UserAnimations
import custom.glyph.arbiter.GlyphArbiter
import custom.glyph.arbiter.QuietRules
import custom.glyph.device.GlyphDevice
import custom.glyph.device.autoLevel
import custom.glyph.device.shell
import custom.glyph.feature.GlyphFeature
import custom.glyph.notification.GlyphNotificationListener
import custom.glyph.notification.NotificationHub
import custom.glyph.output.GlyphLights
import custom.glyph.output.SysfsOutput
import custom.glyph.output.brightnessLevel
import custom.glyph.output.stayAwakeWhile
import custom.glyph.schedule.Schedule
import custom.glyph.schedule.scheduleActivity
import custom.glyph.settings.CoreSettings
import custom.glyph.settings.GlyphSettings
import custom.glyph.signal.events
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

/** The running Glyph core of this process. */
@OptIn(ExperimentalCoroutinesApi::class)
class Glyph(context: Context) {

    private val scope = CoroutineScope(SupervisorJob())

    val device = GlyphDevice.load(context.assets)

    private val shell = device.brightness.shell()

    val settings = GlyphSettings(context)

    val core = CoreSettings(defaultBrightness = shell.default.toFloat() / device.brightness.scale)

    val schedule: Flow<Schedule> = core.schedule(settings)

    val lights = GlyphLights(device.frame.size)

    /** When the Glyphs stay dark, for the arbiter and for lights that are shown beside it. */
    val quietRules: Flow<QuietRules> = combine(settings[core.enabled], schedule.flatMapLatest(context::scheduleActivity), ::QuietRules)

    val arbiter = GlyphArbiter(lights, quietRules, scope)

    val library = AnimationLibrary(
        assets = context.assets,
        codename = device.codename,
        user = UserAnimations(File(context.filesDir, USER_ANIMATIONS), device.codename),
        compositions = CompositionLibrary(context, device),
    )

    val notifications = NotificationHub()

    /** The listener that feeds [notifications]; it also entitles the app to see media sessions. */
    val notificationListener = ComponentName(context, GlyphNotificationListener::class.java)

    init {
        val backLightSensor = device.sensors[GlyphDevice.SensorRole.BACK_LIGHT]
        val backLight = backLightSensor?.let { context.getSystemService(SensorManager::class.java).events(it) } ?: emptyFlow()
        val brightness = brightnessLevel(
            manual = settings[core.brightness],
            auto = if (backLightSensor != null) settings[core.autoBrightness] else flowOf(false),
            lit = lights.lit,
            backLight = backLight,
            autoLevel = { device.brightness.autoLevel(shell, it) },
            initialAuto = core.brightness.default,
        )
        scope.launch(Dispatchers.IO) { lights.drive(SysfsOutput(device.frame, device.indicators), brightness) }
        context.getSystemService(NotificationManager::class.java)
            .setNotificationListenerAccessGranted(notificationListener, true, true)
        scope.launch {
            context.getSystemService(PowerManager::class.java).stayAwakeWhile(arbiter.showing.map { it?.animated == true }, WAKE_TAG)
        }
    }

    /** Runs each feature for the life of the process. */
    fun start(features: List<GlyphFeature>) = features.forEach { scope.launch { it.run() } }

    /** Implemented by the application so every component of the process reaches the same core. */
    interface Host {
        val glyph: Glyph
    }

    private companion object {
        const val WAKE_TAG = "custom.glyph:showing"
        const val USER_ANIMATIONS = "animations"
    }
}

val Context.glyph: Glyph get() = (applicationContext as Glyph.Host).glyph
