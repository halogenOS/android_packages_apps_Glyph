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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import custom.glyph.glyph
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.featureEntries
import custom.glyph.ui.settings.LocalControlsEnabled
import custom.glyph.ui.settings.rememberSetting
import custom.glyph.ui.theme.GlyphTheme

class GlyphActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val host = application as FeatureEntry.Host
        val requestedPage = intent.requestedPage(host.featureEntries)
        setContent {
            GlyphTheme {
                CompositionLocalProvider(LocalControlsEnabled provides rememberSetting(glyph.core.enabled).value) {
                    GlyphScreens(glyph, host, requestedPage, onExit = ::finish)
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
