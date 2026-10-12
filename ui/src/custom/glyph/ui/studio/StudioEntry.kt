/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.studio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import custom.glyph.glyph
import custom.glyph.ui.R
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeaturePage
import custom.glyph.ui.settings.LinkRow

object StudioEntry : FeatureEntry {
    override val id = "studio"

    @Composable
    override fun Row(openPage: () -> Unit) {
        val animations by LocalContext.current.glyph.library.user.entries.collectAsState(emptyList())
        LinkRow(stringResource(R.string.studio), pluralStringResource(R.plurals.studio_animations, animations.size, animations.size), openPage)
    }

    override val page = FeaturePage(R.string.studio) { StudioPage() }
}
