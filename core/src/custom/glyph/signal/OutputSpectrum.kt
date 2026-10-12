/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.signal

import android.media.audiofx.Visualizer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

private const val OUTPUT_MIX = 0

/** How long apart [outputSpectrum] captures arrive. */
val outputSpectrumIntervalMillis: Long get() = 1_000_000L / Visualizer.getMaxCaptureRate()

/**
 * FFT captures of everything the phone plays, in [Visualizer]'s byte layout, at the highest rate
 * it offers. Only the newest capture is kept when the collector falls behind.
 */
fun outputSpectrum(): Flow<ByteArray> = callbackFlow {
    val visualizer = Visualizer(OUTPUT_MIX)
    visualizer.captureSize = Visualizer.getCaptureSizeRange()[1]
    val listener = object : Visualizer.OnDataCaptureListener {
        override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) = Unit

        override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {
            trySend(fft.copyOf())
        }
    }
    visualizer.setDataCaptureListener(listener, Visualizer.getMaxCaptureRate(), false, true)
    visualizer.enabled = true
    awaitClose { visualizer.release() }
}.buffer(Channel.CONFLATED)
