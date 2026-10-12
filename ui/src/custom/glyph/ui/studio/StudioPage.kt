/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.studio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import custom.glyph.animation.Animation
import custom.glyph.animation.AnimationEntry
import custom.glyph.animation.AnimationFile
import custom.glyph.device.GlyphDevice
import custom.glyph.frame.Frame
import custom.glyph.glyph
import custom.glyph.ui.R
import custom.glyph.ui.settings.SettingsCard
import custom.glyph.ui.settings.TextDialog
import kotlinx.coroutines.launch

/** The user's animations: start a new one, open one in the editor, or rename, copy and delete them. */
@Composable
fun StudioPage() {
    val context = LocalContext.current
    val glyph = context.glyph
    val user = glyph.library.user
    val animations by user.entries.collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var naming by remember { mutableStateOf<Naming?>(null) }
    var deleting by remember { mutableStateOf<AnimationEntry?>(null) }

    var refused by remember { mutableStateOf<AnimationFile.Read?>(null) }

    fun open(entry: AnimationEntry) = context.startActivity(StudioActivity.intent(context, entry.id))

    val importing = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            when (val read = context.readAnimationFile(uri)) {
                is AnimationFile.Read.Valid -> open(user.import(read.animation))
                else -> refused = read
            }
        }
    }

    var savingId by rememberSaveable { mutableStateOf<String?>(null) }
    val savingFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(AnimationFile.MIME_TYPE)) { uri ->
        val id = savingId.also { savingId = null }
        if (uri != null && id != null) scope.launch { user.entry(id)?.let { context.saveAnimation(it.animation, uri) } }
    }

    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = { naming = Naming.New }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.studio_new), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = { importing.launch(arrayOf(ANY_FILE)) }) {
            Text(stringResource(R.string.studio_import))
        }
    }
    if (animations.isEmpty()) {
        Text(
            text = stringResource(R.string.studio_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp),
        )
    } else {
        SettingsCard {
            animations.forEach { entry ->
                AnimationRow(
                    entry = entry,
                    onOpen = { open(entry) },
                    onRename = { naming = Naming.Rename(entry) },
                    onDuplicate = { naming = Naming.Duplicate(entry) },
                    onShare = { scope.launch { context.shareAnimation(entry.animation) } },
                    onSave = {
                        savingId = entry.id
                        savingFile.launch(AnimationFile.fileName(entry.animation.name))
                    },
                    onDelete = { deleting = entry },
                )
            }
        }
    }

    refused?.let { read ->
        AlertDialog(
            onDismissRequest = { refused = null },
            title = { Text(stringResource(R.string.import_failed)) },
            text = { importProblem(read)?.let { Text(it) } },
            confirmButton = { TextButton(onClick = { refused = null }) { Text(stringResource(android.R.string.ok)) } },
        )
    }

    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.studio_delete_title, entry.animation.name)) },
            confirmButton = {
                TextButton(onClick = { deleting = null; scope.launch { user.delete(entry.id) } }) { Text(stringResource(R.string.studio_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }

    naming?.let { request ->
        val defaultName = stringResource(R.string.studio_default_name, animations.size + 1)
        TextDialog(
            title = stringResource(request.title),
            initial = when (request) {
                Naming.New -> defaultName
                is Naming.Rename -> request.entry.animation.name
                is Naming.Duplicate -> stringResource(R.string.studio_copy_name, request.entry.animation.name)
            },
            onConfirm = { name ->
                naming = null
                scope.launch {
                    when (request) {
                        Naming.New -> open(user.save(newAnimation(glyph.device, name)))
                        is Naming.Rename -> user.rename(request.entry.id, name)
                        is Naming.Duplicate -> user.duplicate(request.entry.id, name)
                    }
                }
            },
            onDismiss = { naming = null },
        )
    }
}

/** A new animation of one dark step, ready to be filled in. */
private fun newAnimation(device: GlyphDevice, name: String) =
    Animation(Animation.FORMAT, device.codename, name, listOf(Animation.Step(NEW_STEP_MS, Frame.dark(device.frame.size).toList())))

private const val NEW_STEP_MS = 500L

/** Shared files rarely carry a type of their own, so the picker offers every file and the content decides. */
private const val ANY_FILE = "*/*"

/** What a name is being asked for. */
private sealed class Naming(val title: Int) {
    data object New : Naming(R.string.studio_new)
    class Rename(val entry: AnimationEntry) : Naming(R.string.studio_rename)
    class Duplicate(val entry: AnimationEntry) : Naming(R.string.studio_duplicate)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimationRow(
    entry: AnimationEntry,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            Modifier.fillMaxWidth()
                .combinedClickable(onClick = onOpen, onLongClick = { menu = true })
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(entry.animation.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = pluralStringResource(R.plurals.studio_steps, entry.animation.steps.size, entry.animation.steps.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            listOf(R.string.studio_rename to onRename, R.string.studio_duplicate to onDuplicate, R.string.studio_share to onShare, R.string.studio_save_file to onSave, R.string.studio_delete to onDelete)
                .forEach { (label, action) ->
                    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { menu = false; action() })
                }
        }
    }
}
