/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.platform

import android.app.AppOpsManager
import android.content.Context
import android.permission.PermissionManager

/** The app op for audio a sandboxed hotword service receives, which the indicator counts as microphone use. */
val OPSTR_RECEIVE_SANDBOX_TRIGGER_AUDIO: String by lazy { HiddenMembers.constant(AppOpsManager::class, "OPSTR_RECEIVE_SANDBOX_TRIGGER_AUDIO") }

private val attributionChainIdNone: Int by lazy { HiddenMembers.constant(AppOpsManager::class, "ATTRIBUTION_CHAIN_ID_NONE") }
private val attributionFlagsNone: Int by lazy { HiddenMembers.constant(AppOpsManager::class, "ATTRIBUTION_FLAGS_NONE") }
private val attributionFlagAccessor: Int by lazy { HiddenMembers.constant(AppOpsManager::class, "ATTRIBUTION_FLAG_ACCESSOR") }
private val attributionFlagTrusted: Int by lazy { HiddenMembers.constant(AppOpsManager::class, "ATTRIBUTION_FLAG_TRUSTED") }

private val shouldShowPackageForIndicatorCached by lazy {
    HiddenMembers.method(PermissionManager::class, "shouldShowPackageForIndicatorCached", Context::class.java, String::class.java)
}

/** Whether an op is a link of an attribution chain that is neither the accessor nor trusted, which the indicator leaves out. */
fun isUntrustedAttributionLink(attributionFlags: Int, attributionChainId: Int): Boolean =
    attributionChainId != attributionChainIdNone &&
        attributionFlags != attributionFlagsNone &&
        attributionFlags and attributionFlagAccessor == 0 &&
        attributionFlags and attributionFlagTrusted == 0

/** Whether the privacy indicator shows [packageName] at all; it leaves out some system packages. */
fun Context.isShownByPrivacyIndicator(packageName: String): Boolean =
    shouldShowPackageForIndicatorCached.invoke(null, this, packageName) as Boolean
