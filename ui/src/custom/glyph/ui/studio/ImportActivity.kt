/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.studio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import custom.glyph.animation.AnimationFile
import custom.glyph.glyph
import custom.glyph.ui.R
import custom.glyph.ui.theme.GlyphTheme
import kotlinx.coroutines.launch

/** Takes a `.glyph` file another app opens or shares: asks before importing it, or says why it cannot. */
class ImportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.sharedFile() ?: return finish()
        setContent { GlyphTheme { ImportDialog(uri) } }
    }

    @Composable
    private fun ImportDialog(uri: Uri) {
        val read by produceState<AnimationFile.Read?>(null, uri) { value = readAnimationFile(uri) }
        val scope = rememberCoroutineScope()
        when (val file = read) {
            null -> Unit
            is AnimationFile.Read.Valid -> AlertDialog(
                onDismissRequest = ::finish,
                title = { Text(stringResource(R.string.import_title, file.animation.name)) },
                text = { Text(stringResource(R.string.import_summary)) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val entry = glyph.library.user.import(file.animation)
                            startActivity(StudioActivity.intent(this@ImportActivity, entry.id))
                            finish()
                        }
                    }) { Text(stringResource(R.string.studio_import)) }
                },
                dismissButton = { TextButton(onClick = ::finish) { Text(stringResource(android.R.string.cancel)) } },
            )
            else -> AlertDialog(
                onDismissRequest = ::finish,
                title = { Text(stringResource(R.string.import_failed)) },
                text = { importProblem(file)?.let { Text(it) } },
                confirmButton = { TextButton(onClick = ::finish) { Text(stringResource(android.R.string.ok)) } },
            )
        }
    }

    private fun Intent.sharedFile(): Uri? = when (action) {
        Intent.ACTION_VIEW -> data
        Intent.ACTION_SEND -> getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else -> null
    }
}
