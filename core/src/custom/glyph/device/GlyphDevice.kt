/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.device

import android.content.res.AssetManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream

/**
 * Everything the app knows about one phone's Glyph hardware, read from the variant's
 * `device.json`. Coordinates are in the outline's own units; LED positions are indices
 * into the kernel frame. [meters] name the LED rows that show a level, ordered from empty to
 * full; a feature without its meter on a device stays off there. [zoneRoles] name the zone a
 * feature uses by default, such as the one that marks an essential notification. [nodes] name
 * driver files features read, such as whether reverse charging is on. [indicators] name single
 * LEDs outside the frame, such as a recording light. [foreignLayouts] carry light data made for
 * other Nothing phones over to this one. [features] hold settings that belong to a single
 * feature, which decodes its own section with [feature].
 */
@Serializable
data class GlyphDevice(
    val codename: String,
    val frame: FrameNode,
    val outline: Outline,
    val stroke: Float,
    val zones: List<Zone>,
    val brightness: Brightness,
    val sensors: Map<String, String> = emptyMap(),
    val meters: Map<String, List<Int>> = emptyMap(),
    val zoneRoles: Map<String, String> = emptyMap(),
    val nodes: Map<String, String> = emptyMap(),
    val indicators: Map<String, Indicator> = emptyMap(),
    val foreignLayouts: ForeignLayouts = ForeignLayouts(),
    val features: Map<String, JsonElement> = emptyMap(),
) {
    /** The [name]d section of [features] decoded as [T], or null when this device has none. */
    inline fun <reified T> feature(name: String): T? = features[name]?.let { json.decodeFromJsonElement<T>(it) }

    /**
     * Brightness levels on a [scale] where [scale] is full brightness. Each back colour of the
     * phone has its own [Shell] values; the colour is read from [shellColorNode] when present.
     * In a back light sensor event, [levelValue] is the index of its coarse level.
     */
    @Serializable
    data class Brightness(
        val scale: Int,
        val shells: Map<String, Shell>,
        val fallbackShell: String,
        val shellColorNode: String? = null,
        val levelValue: Int = 0,
    )

    /** The brightness a phone starts with, and what auto brightness picks for each sensor level. */
    @Serializable
    data class Shell(val default: Int, val auto: List<AutoStep>)

    /** From the coarse [sensorLevel] upwards, auto brightness uses [brightness]. */
    @Serializable
    data class AutoStep(val sensorLevel: Int, val brightness: Int)

    /**
     * The driver node frames are written to, and optionally the node that wakes the LED chip for
     * them. [setup] holds driver nodes and the values they get once, before the first frame, such
     * as a driver's own brightness that would otherwise scale every frame down.
     */
    @Serializable
    data class FrameNode(
        val node: String,
        val size: Int,
        val maxLevel: Int,
        val power: Power? = null,
        val setup: Map<String, String> = emptyMap(),
    )

    /** Written with [active] before something is lit and with [standby] once everything is dark. */
    @Serializable
    data class Power(val node: String, val active: String, val standby: String)

    /** An LED outside the frame, set by writing its [channel] and a level up to [maxLevel] to [node]. */
    @Serializable
    data class Indicator(val node: String, val channel: Int, val maxLevel: Int)

    @Serializable
    data class Outline(val width: Float, val height: Float, val cornerRadius: Float)

    /**
     * A lit shape on the back. [leds] run along [path] from its first point to its last; a
     * [mirroredRow] zone lists them last to first when they are laid out in a row, so the row
     * reads left to right like the phone.
     */
    @Serializable
    data class Zone(val id: String, val leds: List<Int>, val path: String, val mirroredRow: Boolean = false) {
        /** The LEDs in the order a row shows them. */
        val rowLeds: List<Int> get() = if (mirroredRow) leds.asReversed() else leds
    }

    /** Sensor roles the core understands, mapped in [sensors] to the device's sensor string types. */
    object SensorRole {
        const val BACK_LIGHT = "backLight"
    }

    companion object {
        private const val ASSET = "device.json"

        @PublishedApi
        internal val json = Json { ignoreUnknownKeys = true }

        fun load(assets: AssetManager): GlyphDevice =
            assets.open(ASSET).use(json::decodeFromStream)
    }
}
