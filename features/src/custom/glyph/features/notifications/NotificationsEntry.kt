/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.features.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import custom.glyph.features.R
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeaturePage
import custom.glyph.ui.preview.animationsIn
import custom.glyph.ui.settings.LinkRow
import custom.glyph.ui.settings.rememberSetting

object NotificationsEntry : FeatureEntry {
    override val id = "notifications"

    @Composable
    override fun Row(openPage: () -> Unit) {
        val enabled = rememberSetting(NotificationSettings.enabled)
        val animation = rememberSetting(NotificationSettings.animation)
        val animations by animationsIn(NotificationSettings.CATEGORY)
        val summary = if (enabled.value) animations.firstOrNull { it.id == animation.value }?.animation?.name else stringResource(R.string.off)
        LinkRow(stringResource(R.string.notifications), summary, openPage)
    }

    override val page = FeaturePage(R.string.notifications) { NotificationsPage() }
}
