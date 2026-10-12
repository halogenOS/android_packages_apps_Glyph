/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.signal

import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorPrivacyManager
import android.hardware.SensorPrivacyManager.Sensors
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Handler
import android.os.Looper
import custom.glyph.platform.OPSTR_RECEIVE_SANDBOX_TRIGGER_AUDIO
import custom.glyph.platform.isShownByPrivacyIndicator
import custom.glyph.platform.isUntrustedAttributionLink
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** The app ops the privacy indicator counts as using the microphone. */
private val MICROPHONE_OPS = arrayOf(
    AppOpsManager.OPSTR_RECORD_AUDIO,
    AppOpsManager.OPSTR_PHONE_CALL_MICROPHONE,
    AppOpsManager.OPSTR_RECEIVE_AMBIENT_TRIGGER_AUDIO,
    OPSTR_RECEIVE_SANDBOX_TRIGGER_AUDIO,
    AppOpsManager.OPSTR_RECEIVE_EXPLICIT_USER_INTERACTION_AUDIO,
)

/** How long a single, noted use of the microphone counts, as long as the indicator shows it. */
private const val NOTED_USE_MS = 5_000L

/**
 * The packages using the microphone right now, by the same rules as the privacy indicator in
 * the status bar: ongoing and recently noted microphone app ops, leaving out untrusted links
 * of an attribution chain, apps whose recordings are all silenced, every ongoing use while the
 * microphone is muted or blocked, and the system packages the indicator never shows.
 */
fun Context.microphoneUsers(): Flow<Set<String>> = callbackFlow {
    val appOps = getSystemService(AppOpsManager::class.java)
    val audio = getSystemService(AudioManager::class.java)
    val privacy = getSystemService(SensorPrivacyManager::class.java)
    val handler = Handler(Looper.getMainLooper())
    val use = MicrophoneUse()

    fun publish() {
        trySend(use.users().filterTo(mutableSetOf()) { isShownByPrivacyIndicator(it) })
    }

    fun updateMuted() {
        use.muted = audio.isMicrophoneMute || privacy.isSensorPrivacyEnabled(Sensors.MICROPHONE)
        publish()
    }

    val activeListener = object : AppOpsManager.OnOpActiveChangedListener {
        override fun onOpActiveChanged(op: String, uid: Int, packageName: String, active: Boolean) {
            use.setActive(MicrophoneUse.Op(op, uid, packageName), active)
            publish()
        }

        override fun onOpActiveChanged(
            op: String, uid: Int, packageName: String, attributionTag: String?, virtualDeviceId: Int,
            active: Boolean, attributionFlags: Int, attributionChainId: Int,
        ) {
            if (virtualDeviceId != Context.DEVICE_ID_DEFAULT) return
            if (active && isUntrustedAttributionLink(attributionFlags, attributionChainId)) return
            onOpActiveChanged(op, uid, packageName, active)
        }
    }
    val notedListener = AppOpsManager.OnOpNotedListener { op, uid, packageName, _, _, result ->
        if (result != AppOpsManager.MODE_ALLOWED) return@OnOpNotedListener
        val noted = MicrophoneUse.Op(op, uid, packageName)
        use.noted += noted
        publish()
        handler.removeCallbacksAndMessages(noted)
        handler.postDelayed({ use.noted -= noted; publish() }, noted, NOTED_USE_MS)
    }
    val recordingCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
            use.silencedUids = configs.groupBy { it.clientUid }.filterValues { all -> all.all { it.isClientSilenced } }.keys
            publish()
        }
    }
    val muteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = updateMuted()
    }
    val privacyListener = SensorPrivacyManager.OnSensorPrivacyChangedListener { _, _ -> updateMuted() }

    appOps.getPackagesForOps(MICROPHONE_OPS).forEach { packageOps ->
        packageOps.ops
            .filter { entry -> entry.attributedOpEntries.values.any { it.isRunning } }
            .forEach { use.setActive(MicrophoneUse.Op(it.opStr, packageOps.uid, packageOps.packageName), true) }
    }
    appOps.startWatchingActive(MICROPHONE_OPS, mainExecutor, activeListener)
    appOps.startWatchingNoted(MICROPHONE_OPS, mainExecutor, notedListener)
    audio.registerAudioRecordingCallback(recordingCallback, handler)
    recordingCallback.onRecordingConfigChanged(audio.activeRecordingConfigurations)
    registerReceiver(muteReceiver, IntentFilter(AudioManager.ACTION_MICROPHONE_MUTE_CHANGED), Context.RECEIVER_NOT_EXPORTED)
    privacy.addSensorPrivacyListener(Sensors.MICROPHONE, mainExecutor, privacyListener)
    updateMuted()

    awaitClose {
        appOps.stopWatchingActive(activeListener)
        appOps.stopWatchingNoted(notedListener)
        audio.unregisterAudioRecordingCallback(recordingCallback)
        unregisterReceiver(muteReceiver)
        privacy.removeSensorPrivacyListener(Sensors.MICROPHONE, privacyListener)
        handler.removeCallbacksAndMessages(null)
    }
}.distinctUntilChanged()

/** What is known about microphone use, kept on the main thread. */
private class MicrophoneUse {

    data class Op(val name: String, val uid: Int, val packageName: String)

    private var active = setOf<Op>()
    var noted = setOf<Op>()
    var silencedUids = setOf<Int>()
    var muted = false

    fun setActive(op: Op, isActive: Boolean) {
        active = if (isActive) active + op else active - op
    }

    /** Ongoing uses count unless muted or silenced; a noted use counts for its while regardless. */
    fun users(): Set<String> =
        (active.filterNot { muted || it.uid in silencedUids } + noted).mapTo(mutableSetOf()) { it.packageName }
}
