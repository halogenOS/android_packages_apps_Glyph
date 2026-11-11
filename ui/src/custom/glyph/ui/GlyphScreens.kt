/*
 * SPDX-FileCopyrightText: The halogenOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package custom.glyph.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import custom.glyph.Glyph
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeaturePage
import custom.glyph.ui.feature.FeatureScreen
import custom.glyph.ui.feature.featureEntries
import custom.glyph.ui.home.HomeScreen
import custom.glyph.ui.home.HomeViewModel

/**
 * Shows the home screen, or the page of one feature in front of it. The home screen keeps its saved
 * state, its scroll position among it, while a page is open. A page drops its saved state when it
 * closes, so it opens at the top again.
 */
@Composable
internal fun GlyphScreens(glyph: Glyph, host: FeatureEntry.Host, requestedPage: String?, onExit: () -> Unit) {
    var openPageId by rememberSaveable { mutableStateOf(requestedPage) }
    val screen = host.featureEntries.screenFor(openPageId)
    val screenStates = rememberSaveableStateHolder()
    screenStates.SaveableStateProvider(screen.stateKey) {
        when (screen) {
            GlyphScreen.Home -> HomeScreen(viewModel { HomeViewModel(glyph) }, host.featureSections, onOpen = { openPageId = it.id }, onBack = onExit)
            is GlyphScreen.Page -> {
                val close = {
                    screenStates.removeState(screen.stateKey)
                    openPageId = null
                }
                BackHandler(onBack = close)
                FeatureScreen(screen.content, onBack = close)
            }
        }
    }
}

/** A screen of [GlyphScreens], with the key of its saved state. */
private sealed class GlyphScreen(val stateKey: String) {
    data object Home : GlyphScreen("home")

    class Page(featureId: String, val content: FeaturePage) : GlyphScreen("page/$featureId")
}

/** The page of the first feature with this id, or the home screen when no such feature has a page. */
private fun List<FeatureEntry>.screenFor(featureId: String?): GlyphScreen =
    firstOrNull { it.id == featureId }?.let { feature -> feature.page?.let { GlyphScreen.Page(feature.id, it) } } ?: GlyphScreen.Home
