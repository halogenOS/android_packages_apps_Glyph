/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.sdk

import android.content.Context
import custom.glyph.arbiter.GlyphRequest
import custom.glyph.arbiter.Priority
import custom.glyph.feature.GlyphFeature
import custom.glyph.frame.Frame
import custom.glyph.glyph
import custom.glyph.signal.appsGone
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

/**
 * Shows the frames of the newest Glyph Developer Kit session below everything else the
 * Glyphs do, and ends the sessions of apps that are gone.
 */
class SdkFeature(private val context: Context) : GlyphFeature {

    private val glyph = context.glyph
    private val sessions = context.sdkSessions

    override suspend fun run() = coroutineScope {
        launch { forgetGoneApps() }
        combine(glyph.settings[SdkSettings.enabled], sessions.state) { enabled, state -> state.shown.takeIf { enabled } }
            .distinctUntilChanged { old, new -> old?.uid == new?.uid }
            .collectLatest { session ->
                if (session != null) glyph.arbiter.show(GlyphRequest("$SOURCE ${session.packageName}", Priority.SDK, frames(session.uid), animated = false))
            }
    }

    private fun frames(uid: Int): Flow<Frame> = sessions.state
        .mapNotNull { state -> state.session(uid)?.levels?.let { glyph.device.foreignLayouts.forModel(it, state.models[uid], glyph.device.frame.size) } }
        .distinctUntilChanged()

    private suspend fun forgetGoneApps() = sessions.state
        .map { it.open.isNotEmpty() }
        .distinctUntilChanged()
        .collectLatest { watching -> if (watching) context.appsGone().collect(sessions::forget) }

    private companion object {
        const val SOURCE = "sdk"
    }
}
