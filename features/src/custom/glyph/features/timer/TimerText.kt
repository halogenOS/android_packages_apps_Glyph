/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.timer

import android.text.format.DateUtils

/** A timer length as a clock shows it, such as 4:59, rounded up so that 0:00 means the time is up. */
fun timerText(millis: Long): String = DateUtils.formatElapsedTime((millis + 999) / 1000)
