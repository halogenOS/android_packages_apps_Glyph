/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.home

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import custom.glyph.Glyph
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Priority
import custom.glyph.device.GlyphDevice
import custom.glyph.frame.Frame
import custom.glyph.settings.Setting
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The main screen: core settings, and the previews it shows on the Glyphs. Every preview is a
 * request to the arbiter held by one coroutine, so ending the coroutine withdraws the request.
 */
class HomeViewModel(private val glyph: Glyph) : ViewModel() {

    val device = glyph.device

    val frame = glyph.lights.frame

    val enabled = glyph.core.enabled.state()
    val brightness = glyph.core.brightness.state()
    val autoBrightness = glyph.core.autoBrightness.state()

    /** Whether the phone can judge the light around its back, which automatic brightness needs. */
    val hasAutoBrightness = GlyphDevice.SensorRole.BACK_LIGHT in device.sensors

    private val sketch = MutableStateFlow(Frame.dark(device.frame.size))
    private var brightnessPreview: Job? = null
    private var brightnessPreviewUntil = 0L

    init {
        viewModelScope.launch {
            sketch.map { !it.isDark }.distinctUntilChanged().collectLatest { lit ->
                if (lit) preview(SKETCH, sketch)
            }
        }
    }

    fun toggle(zone: GlyphDevice.Zone) {
        val current = sketch.value
        sketch.value = current.with(zone.leds, if (zone.leds.any { current[it] > 0f }) 0f else 1f)
    }

    fun setEnabled(value: Boolean) = save(glyph.core.enabled, value)

    fun setAutoBrightness(value: Boolean) = save(glyph.core.autoBrightness, value)

    /**
     * Stores the level and keeps every Glyph lit at it until the user has stopped choosing for a
     * moment. One request stays up the whole time, so the Glyphs never go dark between two steps.
     */
    fun setBrightness(value: Float) {
        save(glyph.core.brightness, value)
        brightnessPreviewUntil = SystemClock.uptimeMillis() + BRIGHTNESS_PREVIEW_MS
        if (brightnessPreview?.isActive == true) return
        brightnessPreview = viewModelScope.launch { preview(BRIGHTNESS, allLitWhileChoosing()) }
    }

    private fun allLitWhileChoosing(): Flow<Frame> = flow {
        emit(Frame.full(device.frame.size))
        while (SystemClock.uptimeMillis() < brightnessPreviewUntil) {
            delay(brightnessPreviewUntil - SystemClock.uptimeMillis())
        }
    }

    /** Ends every preview, for when the screen is left. */
    fun clear() {
        brightnessPreview?.cancel()
        sketch.value = Frame.dark(device.frame.size)
    }

    private suspend fun preview(source: String, frames: Flow<Frame>) =
        glyph.arbiter.show(GlyphRequest(source, Priority.PREVIEW, frames))

    private fun <T> save(setting: Setting<T>, value: T) {
        viewModelScope.launch { glyph.settings.set(setting, value) }
    }

    private fun <T> Setting<T>.state(): StateFlow<T> =
        glyph.settings[this].stateIn(viewModelScope, SharingStarted.Eagerly, default)

    private companion object {
        const val SKETCH = "preview:sketch"
        const val BRIGHTNESS = "preview:brightness"
        const val BRIGHTNESS_PREVIEW_MS = 1500L
    }
}
