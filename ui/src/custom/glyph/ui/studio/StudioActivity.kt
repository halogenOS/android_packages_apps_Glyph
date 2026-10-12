/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.studio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import custom.glyph.glyph
import custom.glyph.ui.theme.GlyphTheme
import kotlinx.coroutines.launch

/** Edits one of the user's animations, showing it on the Glyphs while the editor is in view. */
class StudioActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels {
        viewModelFactory { initializer { EditorViewModel(glyph, requireNotNull(intent.getStringExtra(EXTRA_ID))) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GlyphTheme { EditorScreen(viewModel, onClose = ::finish) } }
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.showOnGlyphs() } }
    }

    companion object {
        private const val EXTRA_ID = "id"

        fun intent(context: Context, id: String): Intent = Intent(context, StudioActivity::class.java).putExtra(EXTRA_ID, id)
    }
}
