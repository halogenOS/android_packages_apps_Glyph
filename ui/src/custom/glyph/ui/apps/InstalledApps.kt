/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import java.text.Collator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An app the user can open, as the settings show it. */
class InstalledApp(val packageName: String, val label: String, val icon: ImageBitmap)

/**
 * The apps with a launcher entry, sorted by name, empty until they are read. With a
 * [permission], only the apps that ask for it.
 */
@Composable
fun installedApps(permission: String? = null): State<List<InstalledApp>> {
    val context = LocalContext.current
    return produceState(emptyList(), permission) { value = withContext(Dispatchers.IO) { context.launcherApps(permission) } }
}

/** An app with its icon and a switch; tapping the row itself calls [onClick]. */
@Composable
fun AppSwitchRow(app: InstalledApp, summary: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(app.label, style = MaterialTheme.typography.titleMedium)
            summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun Context.launcherApps(permission: String?): List<InstalledApp> {
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val collator = Collator.getInstance()
    return packageManager.queryIntentActivities(launcher, PackageManager.ResolveInfoFlags.of(0))
        .distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != packageName }
        .filter { permission == null || permission in requestedPermissions(it.activityInfo.packageName) }
        .map { info ->
            val app = info.activityInfo.applicationInfo
            InstalledApp(
                packageName = app.packageName,
                label = app.loadLabel(packageManager).toString(),
                icon = app.loadIcon(packageManager).toBitmap(ICON_PX, ICON_PX).asImageBitmap(),
            )
        }
        .sortedWith(compareBy(collator) { it.label })
}

private fun Context.requestedPermissions(packageName: String): List<String> =
    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        .requestedPermissions.orEmpty().asList()

private const val ICON_PX = 120
