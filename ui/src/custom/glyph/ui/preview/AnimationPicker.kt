/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.preview

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import custom.glyph.animation.AnimationEntry
import custom.glyph.animation.CompositionLibrary
import custom.glyph.animation.UserAnimations
import custom.glyph.glyph
import custom.glyph.ui.R

/** What one can pick for [category]: its animations, then the user's own and their compositions; empty until they are read. */
@Composable
fun animationsIn(category: String): State<List<AnimationEntry>> {
    val library = LocalContext.current.glyph.library
    return produceState(emptyList(), category) { value = library.choices(category) }
}

/**
 * One row per animation; choosing one selects it and plays it once on the Glyphs. Tapping the
 * one that is playing stops it. When the animations come from more than one place (the phone,
 * the Studio, the Glyph Composer), each place is a group that opens and closes; the group
 * holding the selected animation starts open.
 */
@Composable
fun AnimationPicker(
    animations: List<AnimationEntry>,
    selectedId: String,
    onSelect: (AnimationEntry) -> Unit,
    enabled: Boolean = true,
) {
    val preview = rememberAnimationPreview()
    val playing by preview.playing.collectAsStateWithLifecycle()
    val groups = animations.groupBy { it.category }

    @Composable
    fun Choice(entry: AnimationEntry) = AnimationChoice(entry, entry.id == selectedId, entry == playing, enabled) {
        onSelect(entry)
        preview.toggle(entry)
    }

    if (groups.size <= 1) {
        animations.forEach { Choice(it) }
        return
    }
    val selectedGroup = animations.firstOrNull { it.id == selectedId }?.category
    var open by remember(selectedGroup) { mutableStateOf(setOfNotNull(selectedGroup)) }
    groups.forEach { (category, entries) ->
        val isOpen = category in open
        GroupHeader(stringResource(groupTitle(category)), entries.size, isOpen) { open = if (isOpen) open - category else open + category }
        if (isOpen) entries.forEach { Choice(it) }
    }
}

@Composable
private fun AnimationChoice(entry: AnimationEntry, selected: Boolean, playing: Boolean, enabled: Boolean, onChoose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onChoose)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(
            text = entry.animation.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

/** A group's title and size; tapping it opens or closes the group. */
@Composable
private fun GroupHeader(title: String, size: Int, open: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        Text("$size", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(
            imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** What a group of animations is called, by where they come from. */
@StringRes
private fun groupTitle(category: String): Int = when (category) {
    UserAnimations.CATEGORY -> R.string.my_animations
    CompositionLibrary.CATEGORY -> R.string.compositions
    else -> R.string.built_in_animations
}
