/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph

import android.app.Application
import custom.glyph.features.calls.CallFeature
import custom.glyph.features.calls.CallsEntry
import custom.glyph.features.charging.ChargingEntry
import custom.glyph.features.charging.ChargingFeature
import custom.glyph.features.charging.ReverseChargingFeature
import custom.glyph.features.flip.FlipEntry
import custom.glyph.features.flip.FlipFeature
import custom.glyph.features.microphone.MicrophoneEntry
import custom.glyph.features.microphone.MicrophoneLightFeature
import custom.glyph.features.notifications.NotificationFeature
import custom.glyph.features.notifications.NotificationsEntry
import custom.glyph.features.progress.ProgressEntry
import custom.glyph.features.progress.ProgressFeature
import custom.glyph.features.sdk.SdkEntry
import custom.glyph.features.sdk.SdkFeature
import custom.glyph.features.sdk.SdkSessions
import custom.glyph.features.timer.GlyphTimer
import custom.glyph.features.timer.TimerEntry
import custom.glyph.features.timer.TimerFeature
import custom.glyph.features.torch.GlyphTorch
import custom.glyph.features.torch.TorchFeature
import custom.glyph.features.visualizer.MusicVisualizerFeature
import custom.glyph.features.visualizer.VisualizerEntry
import custom.glyph.features.volume.VolumeEntry
import custom.glyph.features.volume.VolumeFeature
import custom.glyph.ui.R
import custom.glyph.ui.animations.BuiltInAnimationsEntry
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeatureSection
import custom.glyph.ui.schedule.ScheduleEntry
import custom.glyph.ui.studio.StudioEntry

class GlyphApplication : Application(), Glyph.Host, GlyphTimer.Host, GlyphTorch.Host, SdkSessions.Host, FeatureEntry.Host {

    override val glyph by lazy { Glyph(this) }

    override val glyphTimer = GlyphTimer()

    override val glyphTorch = GlyphTorch()

    override val sdkSessions = SdkSessions()

    override val featureSections = listOf(
        FeatureSection(null, listOf(ScheduleEntry)),
        FeatureSection(R.string.section_alerts, listOf(CallsEntry, NotificationsEntry, FlipEntry)),
        FeatureSection(R.string.section_live, listOf(TimerEntry, ProgressEntry, ChargingEntry, VolumeEntry, MicrophoneEntry)),
        FeatureSection(R.string.section_music_and_apps, listOf(VisualizerEntry, SdkEntry)),
        FeatureSection(R.string.section_animations, listOf(StudioEntry, BuiltInAnimationsEntry)),
    )

    override fun onCreate() {
        super.onCreate()
        glyph.start(
            listOf(
                CallFeature(this),
                NotificationFeature(this),
                FlipFeature(this),
                VolumeFeature(this),
                ChargingFeature(this),
                ReverseChargingFeature(this),
                MusicVisualizerFeature(this),
                ProgressFeature(this),
                TimerFeature(this),
                MicrophoneLightFeature(this),
                TorchFeature(this),
                SdkFeature(this),
            )
        )
    }
}
