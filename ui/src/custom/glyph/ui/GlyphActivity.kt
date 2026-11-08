/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import custom.glyph.glyph
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeatureScreen
import custom.glyph.ui.feature.featureEntries
import custom.glyph.ui.home.HomeScreen
import custom.glyph.ui.home.HomeViewModel
import custom.glyph.ui.settings.LocalControlsEnabled
import custom.glyph.ui.settings.rememberSetting
import custom.glyph.ui.theme.GlyphTheme

class GlyphActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val host = application as FeatureEntry.Host
        val features = host.featureEntries
        setContent {
            GlyphTheme {
                var openId by rememberSaveable { mutableStateOf(intent.requestedPage(features)) }
                val open = features.firstOrNull { it.id == openId }?.page
                CompositionLocalProvider(LocalControlsEnabled provides rememberSetting(glyph.core.enabled).value) {
                    if (open == null) {
                        HomeScreen(viewModel { HomeViewModel(glyph) }, host.featureSections, onOpen = { openId = it.id }, onBack = ::finish)
                    } else {
                        BackHandler { openId = null }
                        FeatureScreen(open, onBack = { openId = null })
                    }
                }
            }
        }
    }

    /** The page a link or a long-pressed tile asks for, or null for the main screen. */
    private fun Intent.requestedPage(features: List<FeatureEntry>): String? =
        getStringExtra(EXTRA_PAGE)
            ?: getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)
                ?.let { tile -> features.firstOrNull { it.tile?.name == tile.className }?.id }

    companion object {
        private const val EXTRA_PAGE = "page"

        /** Opens the page of the feature with this id. */
        fun pageIntent(context: Context, featureId: String): Intent =
            Intent(context, GlyphActivity::class.java).putExtra(EXTRA_PAGE, featureId)
    }
}
