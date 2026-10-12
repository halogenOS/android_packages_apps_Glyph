/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.tile

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.quicksettings.TileService

/** Lists [tile] among the Quick Settings tiles one can add only while it is [offered]. */
fun Context.offerTile(tile: Class<out TileService>, offered: Boolean) =
    packageManager.setComponentEnabledSetting(
        ComponentName(this, tile),
        if (offered) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP,
    )
