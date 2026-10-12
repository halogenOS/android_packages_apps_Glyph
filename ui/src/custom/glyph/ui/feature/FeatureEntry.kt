/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.feature

import android.service.quicksettings.TileService
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

/**
 * How one feature appears on the main screen. A feature with a setting or two shows them right
 * there; one with more shows a row that opens its [page].
 */
interface FeatureEntry {
    val id: String

    @Composable
    fun Row(openPage: () -> Unit)

    val page: FeaturePage?
        get() = null

    /** The Quick Settings tile of this feature; long-pressing it opens the [page]. */
    val tile: Class<out TileService>?
        get() = null

    /** Implemented by the application, which decides which features exist and how they are grouped. */
    interface Host {
        val featureSections: List<FeatureSection>
    }
}

/** Entries shown together on the main screen, under [title] when there is one. */
class FeatureSection(@StringRes val title: Int?, val entries: List<FeatureEntry>)

/** Every entry of every section, in order. */
val FeatureEntry.Host.featureEntries: List<FeatureEntry> get() = featureSections.flatMap { it.entries }

/** A feature's own page, under [title]. */
class FeaturePage(@StringRes val title: Int, val content: @Composable () -> Unit)
