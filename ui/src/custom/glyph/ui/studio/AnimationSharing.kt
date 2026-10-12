/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.studio

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import custom.glyph.animation.Animation
import custom.glyph.animation.AnimationFile
import custom.glyph.glyph
import custom.glyph.ui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Offers [animation] to other apps as a `.glyph` file. Only a copy in the app's cache is
 * handed out, replacing whatever was shared before.
 */
suspend fun Context.shareAnimation(animation: Animation) {
    val file = withContext(Dispatchers.IO) {
        File(cacheDir, SHARED).apply { deleteRecursively(); mkdirs() }
            .resolve(AnimationFile.fileName(animation.name))
            .apply { writeBytes(AnimationFile.encode(animation)) }
    }
    val uri = FileProvider.getUriForFile(this, "$packageName$AUTHORITY_SUFFIX", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        setType(AnimationFile.MIME_TYPE)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(animation.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(send, animation.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Writes [animation] as a `.glyph` file to [uri], a place the user picked. */
suspend fun Context.saveAnimation(animation: Animation, uri: Uri) = withContext(Dispatchers.IO) {
    contentResolver.openOutputStream(uri, WRITE_TRUNCATE)?.use { it.write(AnimationFile.encode(animation)) }
}

/** What the file at [uri] holds for this phone; unreadable files hold no animation. */
suspend fun Context.readAnimationFile(uri: Uri): AnimationFile.Read = withContext(Dispatchers.IO) {
    val bytes = runCatching { contentResolver.openInputStream(uri)?.use { it.readNBytes(MAX_FILE_BYTES) } }.getOrNull()
        ?: return@withContext AnimationFile.Read.NotAnimation
    AnimationFile.read(bytes, glyph.device.codename, glyph.device.frame.size)
}

/** Why a file cannot be imported, or null when it can. */
@Composable
fun importProblem(read: AnimationFile.Read): String? = when (read) {
    is AnimationFile.Read.Valid -> null
    AnimationFile.Read.NotAnimation -> stringResource(R.string.import_not_animation)
    AnimationFile.Read.Newer -> stringResource(R.string.import_newer)
    is AnimationFile.Read.OtherDevice -> stringResource(R.string.import_other_device, read.device, LocalContext.current.glyph.device.codename)
}

private const val SHARED = "shared"
private const val WRITE_TRUNCATE = "wt"
private const val AUTHORITY_SUFFIX = ".files"
private const val MAX_FILE_BYTES = 8 * 1024 * 1024
