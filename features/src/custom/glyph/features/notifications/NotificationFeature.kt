/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.notifications

import android.app.Notification
import android.content.Context
import custom.glyph.animation.frames
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Preemption
import custom.glyph.arbiter.Priority
import custom.glyph.arbiter.collectWhenIdle
import custom.glyph.feature.GlyphFeature
import custom.glyph.frame.Frame
import custom.glyph.glyph
import custom.glyph.notification.ActiveNotification
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Plays the chosen animation when a notification alerts, and keeps a zone lit for each
 * essential app as long as one of its notifications is in the shade.
 */
class NotificationFeature(private val context: Context) : GlyphFeature {

    private val glyph = context.glyph
    private val settings = glyph.settings

    override suspend fun run() = coroutineScope {
        launch { flashOnAlerts() }
        keepEssentialLit()
    }

    private data class AlertRules(val enabled: Boolean, val animationId: String, val muted: Set<String>, val soundLights: Boolean)

    /** Each alert plays the chosen animation, or the lights of its sound when that is a composition and they are wanted. */
    private suspend fun flashOnAlerts() {
        combine(
            settings[NotificationSettings.enabled],
            settings[NotificationSettings.animation],
            settings[NotificationSettings.mutedApps],
            settings[NotificationSettings.soundLights],
            ::AlertRules,
        ).collectLatest { rules ->
            if (!rules.enabled) return@collectLatest
            val animation = (glyph.library.entry(rules.animationId) ?: glyph.library.entry(NotificationSettings.animation.default))?.animation
                ?: return@collectLatest
            glyph.notifications.alerts
                .filter { it.lightsGlyphs && it.packageName !in rules.muted }
                .collectWhenIdle { alert ->
                    val lights = alert.sound?.takeIf { rules.soundLights }?.let { glyph.library.compositions.animation(it) } ?: animation
                    glyph.arbiter.show(GlyphRequest(SOURCE, Priority.NOTIFICATION, lights.frames(), Preemption.DROP))
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun keepEssentialLit() {
        combine(settings[NotificationSettings.enabled], settings[NotificationSettings.essentialApps], glyph.notifications.active) { enabled, apps, active ->
            if (enabled) active.values.filter { it.lightsGlyphs && it.packageName in apps }.mapTo(sortedSetOf()) { it.packageName } else emptySet()
        }
            .distinctUntilChanged()
            .flatMapLatest { apps -> essentialZones(apps) }
            .distinctUntilChanged()
            .collectLatest { zoneIds ->
                if (zoneIds.isNotEmpty()) {
                    glyph.arbiter.show(GlyphRequest(ESSENTIAL_SOURCE, Priority.ESSENTIAL, essentialFrames(zoneIds), animated = false))
                }
            }
    }

    private fun essentialZones(apps: Set<String>): Flow<Set<String>> =
        if (apps.isEmpty()) flowOf(emptySet()) else combine(apps.map { settings[NotificationSettings.essentialZone(it, glyph.device)] }) { it.toSet() }

    /** The zones fade in to a soft glow and stay until the request ends. */
    private fun essentialFrames(zoneIds: Set<String>): Flow<Frame> = flow {
        val leds = glyph.device.zones.filter { it.id in zoneIds }.flatMap { it.leds }
        val dark = Frame.dark(glyph.device.frame.size)
        for (step in 1..ESSENTIAL_FADE_STEPS) {
            emit(dark.with(leds, NotificationSettings.ESSENTIAL_LEVEL * step / ESSENTIAL_FADE_STEPS))
            delay(ESSENTIAL_FADE_STEP_MS)
        }
        awaitCancellation()
    }

    /** Whether this notification may light the Glyphs at all. */
    private val ActiveNotification.lightsGlyphs: Boolean
        get() = audible && !ongoing && category != Notification.CATEGORY_CALL && packageName !in ignoredPackages

    private val ignoredPackages = setOf(ANDROID, SYSTEM_UI, context.packageName)

    private companion object {
        const val SOURCE = "notification"
        const val ESSENTIAL_SOURCE = "notification:essential"
        const val ANDROID = "android"
        const val SYSTEM_UI = "com.android.systemui"
        const val ESSENTIAL_FADE_STEPS = 5
        const val ESSENTIAL_FADE_STEP_MS = 17L
    }
}
