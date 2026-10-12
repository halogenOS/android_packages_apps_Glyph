/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.sdk

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * What Glyph Developer Kit apps asked for: the phone model each app registered for and the
 * sessions they hold open, oldest first. Only an open session shows the frames it sends.
 */
class SdkSessions {

    class Session(val uid: Int, val packageName: String, val levels: IntArray? = null)

    data class State(val models: Map<Int, String> = emptyMap(), val open: List<Session> = emptyList()) {

        /** The newest session that has sent a frame; it owns the app's share of the Glyphs. */
        val shown: Session? get() = open.lastOrNull { it.levels != null }

        fun session(uid: Int): Session? = open.firstOrNull { it.uid == uid }
    }

    private val current = MutableStateFlow(State())

    val state: StateFlow<State> = current.asStateFlow()

    fun register(uid: Int, model: String) = current.update { it.copy(models = it.models + (uid to model)) }

    fun open(uid: Int, packageName: String) = current.update { state ->
        if (state.session(uid) != null) state else state.copy(open = state.open + Session(uid, packageName))
    }

    fun show(uid: Int, levels: IntArray) = current.update { state ->
        state.copy(open = state.open.map { if (it.uid == uid) Session(uid, it.packageName, levels) else it })
    }

    fun close(uid: Int) = current.update { state -> state.copy(open = state.open.filterNot { it.uid == uid }) }

    /** Drops everything of an app whose processes are gone. */
    fun forget(uid: Int) = current.update { state -> State(state.models - uid, state.open.filterNot { it.uid == uid }) }

    /** Implemented by the application so the bridge and the lights share one set of sessions. */
    interface Host {
        val sdkSessions: SdkSessions
    }
}

val Context.sdkSessions: SdkSessions get() = (applicationContext as SdkSessions.Host).sdkSessions
