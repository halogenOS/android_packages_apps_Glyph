/*
 * SPDX-FileCopyrightText: The halogenOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package custom.glyph.ui.settings

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/**
 * Whether the controls below can be used. Rows, pickers and the preview read it themselves, so a
 * screen turns all of them off at once without any of them being told.
 */
val LocalControlsEnabled = compositionLocalOf { true }

/** Draws content at the strength Material gives disabled content while [enabled] is false. */
fun Modifier.dimmedUnless(enabled: Boolean): Modifier = if (enabled) this else alpha(DISABLED_CONTENT_ALPHA)

private const val DISABLED_CONTENT_ALPHA = 0.38f
