/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.studio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import custom.glyph.animation.Animation
import custom.glyph.frame.Frame
import custom.glyph.ui.R
import custom.glyph.ui.preview.GlyphGeometry
import custom.glyph.ui.preview.GlyphPreview
import custom.glyph.ui.settings.TextDialog
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** The step editor: the drawing of the open step, the strip of all steps, and the open step's settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: EditorViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val geometry = remember(viewModel.device) { GlyphGeometry(viewModel.device) }
    var leaving by remember { mutableStateOf(false) }

    fun leave() = if (state?.unsaved == true) leaving = true else onClose()

    BackHandler(onBack = ::leave)
    val editor = state ?: return
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(editor.animation.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = ::leave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.setPlaying(!editor.playing) }) {
                        Text(stringResource(if (editor.playing) R.string.studio_stop else R.string.studio_play))
                    }
                    TextButton(onClick = { viewModel.save() }, enabled = editor.unsaved) { Text(stringResource(R.string.studio_save)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlyphPreview(
                geometry = geometry,
                frame = editor.frame,
                selected = editor.selected,
                modifier = Modifier.weight(1f).padding(top = 8.dp),
                onZoneTap = viewModel::tapZone,
            )
            editor.strip?.let { strip ->
                LedRow(strip.rowLeds, editor.current.levels, editor.selected, viewModel::toggleLed, viewModel::addLeds)
            }
            SelectionControls(editor, viewModel)
            StepStrip(editor, geometry, viewModel::openStep)
            StepControls(editor, viewModel)
        }
    }

    if (leaving) {
        AlertDialog(
            onDismissRequest = { leaving = false },
            title = { Text(stringResource(R.string.studio_unsaved)) },
            confirmButton = { TextButton(onClick = { leaving = false; viewModel.save(onClose) }) { Text(stringResource(R.string.studio_save)) } },
            dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.studio_discard)) } },
        )
    }
}

/** Whether taps pick zones or single LEDs, and how bright the picked LEDs are in the open step. */
@Composable
private fun SelectionControls(editor: EditorState, viewModel: EditorViewModel) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = editor.singleLeds,
            onClick = { viewModel.setSingleLeds(!editor.singleLeds) },
            label = { Text(stringResource(R.string.studio_single_leds)) },
        )
        Text(
            text = when {
                editor.selected.isNotEmpty() -> pluralStringResource(R.plurals.studio_picked, editor.selected.size, editor.selected.size)
                editor.singleLeds && editor.strip == null -> stringResource(R.string.studio_strip_hint)
                else -> stringResource(R.string.studio_pick_hint)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        TextButton(onClick = viewModel::clearSelection, enabled = editor.selected.isNotEmpty()) { Text(stringResource(R.string.studio_clear)) }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.brightness), style = MaterialTheme.typography.titleSmall)
        Slider(
            value = editor.selectionLevel ?: 0f,
            onValueChange = viewModel::setLevel,
            enabled = editor.selected.isNotEmpty() && !editor.playing,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        Text("${((editor.selectionLevel ?: 0f) * 100).roundToInt()} %", style = MaterialTheme.typography.bodyMedium)
    }
}

/** Every step as a small drawing with its length; the open one is outlined and kept in view. */
@Composable
private fun StepStrip(editor: EditorState, geometry: GlyphGeometry, onOpen: (Int) -> Unit) {
    val list = rememberLazyListState()
    LaunchedEffect(editor.step) { list.animateScrollToItem(editor.step) }
    LazyRow(state = list, horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        itemsIndexed(editor.animation.steps) { index, step ->
            val open = index == editor.step
            Card(
                modifier = Modifier.clickable { onOpen(index) },
                border = if (open) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    GlyphPreview(geometry, Frame.of(step.levels), Modifier.height(THUMBNAIL_HEIGHT))
                    Text(stringResource(R.string.studio_millis, step.duration), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** Adding, removing and moving steps, and the open step's length and fade. */
@Composable
private fun StepControls(editor: EditorState, viewModel: EditorViewModel) {
    var typingLength by remember { mutableStateOf(false) }
    val enabled = !editor.playing
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(onClick = { viewModel.moveStep(-1) }, enabled = enabled && editor.step > 0) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.studio_move_left))
        }
        IconButton(onClick = viewModel::addStep, enabled = enabled) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.studio_add_step))
        }
        IconButton(onClick = viewModel::deleteStep, enabled = enabled && editor.animation.steps.size > 1) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.studio_delete_step))
        }
        IconButton(onClick = { viewModel.moveStep(1) }, enabled = enabled && editor.step < editor.animation.steps.lastIndex) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.studio_move_right))
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.studio_length), style = MaterialTheme.typography.titleSmall)
        Slider(
            value = editor.current.duration.toSliderPosition(),
            onValueChange = { viewModel.setDuration(it.toMillis()) },
            enabled = enabled,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        TextButton(onClick = { typingLength = true }, enabled = enabled) {
            Text(stringResource(R.string.studio_millis, editor.current.duration))
        }
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.studio_fade), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Switch(
            checked = editor.current.transition == Animation.Transition.FADE,
            onCheckedChange = viewModel::setFade,
            enabled = enabled,
        )
    }

    if (typingLength) {
        TextDialog(
            title = stringResource(R.string.studio_length_millis),
            initial = editor.current.duration.toString(),
            onConfirm = { text -> typingLength = false; text.toLongOrNull()?.let(viewModel::setDuration) },
            onDismiss = { typingLength = false },
        )
    }
}

/** Step lengths sit on the slider logarithmically, so short steps get as much room as long ones. */
private val LENGTH_RANGE = ln(EditorViewModel.MAX_STEP_MS.toDouble() / EditorViewModel.MIN_STEP_MS)

private fun Long.toSliderPosition(): Float = (ln(toDouble() / EditorViewModel.MIN_STEP_MS) / LENGTH_RANGE).toFloat().coerceIn(0f, 1f)

private fun Float.toMillis(): Long =
    (EditorViewModel.MIN_STEP_MS * (EditorViewModel.MAX_STEP_MS.toDouble() / EditorViewModel.MIN_STEP_MS).pow(toDouble())).roundToLong()

private val THUMBNAIL_HEIGHT = 72.dp
