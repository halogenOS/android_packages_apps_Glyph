/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.animations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import custom.glyph.animation.AnimationEntry
import custom.glyph.glyph
import custom.glyph.ui.R
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeaturePage
import custom.glyph.ui.preview.rememberAnimationPreview
import custom.glyph.ui.settings.LinkRow

/** Every animation that comes with the phone, to look at on the Glyphs. */
object BuiltInAnimationsEntry : FeatureEntry {
    override val id = "animations"

    @Composable
    override fun Row(openPage: () -> Unit) {
        LinkRow(stringResource(R.string.built_in_animations), stringResource(R.string.built_in_animations_summary), openPage)
    }

    override val page = FeaturePage(R.string.built_in_animations) { BuiltInAnimationsPage() }
}

/** The animations as chips, grouped by what they are for; tapping one plays it once, tapping it again stops it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuiltInAnimationsPage() {
    val library = LocalContext.current.glyph.library
    val animations by produceState(emptyMap<String, List<AnimationEntry>>()) { value = library.builtIn().groupBy { it.category } }
    val preview = rememberAnimationPreview()
    val playing by preview.playing.collectAsStateWithLifecycle()
    animations.forEach { (category, entries) ->
        Text(
            text = categoryLabel(category),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { entry ->
                FilterChip(selected = entry == playing, onClick = { preview.toggle(entry) }, label = { Text(entry.animation.name) })
            }
        }
    }
}

@Composable
private fun categoryLabel(category: String): String = when (category) {
    "call" -> stringResource(R.string.category_call)
    "notification" -> stringResource(R.string.category_notification)
    "system" -> stringResource(R.string.category_system)
    else -> category
}
