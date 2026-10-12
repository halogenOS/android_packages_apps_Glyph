/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.platform

import android.media.AudioManager

private val getRingerModeInternal by lazy { HiddenMembers.method(AudioManager::class, "getRingerModeInternal") }
private val setRingerModeInternal by lazy { HiddenMembers.method(AudioManager::class, "setRingerModeInternal", Int::class.javaPrimitiveType!!) }

/**
 * The ringer mode as the user set it, like the volume panel does. Unlike the public ringer mode,
 * changing it to or from silent leaves Do Not Disturb alone.
 */
var AudioManager.internalRingerMode: Int
    get() = getRingerModeInternal.invoke(this) as Int
    set(mode) {
        setRingerModeInternal.invoke(this, mode)
    }

/** The extra of a volume change broadcast that holds the stream's volume before the change. */
val EXTRA_PREVIOUS_VOLUME: String by lazy { HiddenMembers.constant(AudioManager::class, "EXTRA_PREV_VOLUME_STREAM_VALUE") }
