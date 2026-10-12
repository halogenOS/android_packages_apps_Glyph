/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import custom.glyph.ui.R
import custom.glyph.ui.feature.FeatureEntry
import custom.glyph.ui.feature.FeatureSection
import custom.glyph.ui.preview.GlyphGeometry
import custom.glyph.ui.preview.GlyphPreview
import custom.glyph.ui.settings.SettingsCard
import custom.glyph.ui.settings.SwitchRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, sections: List<FeatureSection>, onOpen: (FeatureEntry) -> Unit, onBack: () -> Unit) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val brightness by viewModel.brightness.collectAsStateWithLifecycle()
    val autoBrightness by viewModel.autoBrightness.collectAsStateWithLifecycle()
    val geometry = remember(viewModel.device) { GlyphGeometry(viewModel.device) }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.clear() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.glyph_interface)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlyphPreview(
                geometry = geometry,
                frame = frame,
                modifier = Modifier.weight(PREVIEW_WEIGHT).padding(top = 8.dp),
                onZoneTap = { viewModel.toggle(it) },
            )
            Text(
                text = stringResource(R.string.preview_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item(key = "enabled") {
                    SettingsCard {
                        SwitchRow(stringResource(R.string.use_glyphs), enabled, viewModel::setEnabled)
                    }
                }
                item(key = "brightness") {
                    BrightnessSettings(brightness, autoBrightness, viewModel.hasAutoBrightness, viewModel::setBrightness, viewModel::setAutoBrightness)
                }
                sections.forEachIndexed { index, section ->
                    item(key = "section:$index") {
                        section.title?.let { SectionTitle(stringResource(it)) }
                        SettingsCard {
                            section.entries.forEach { feature ->
                                feature.Row(openPage = {
                                    viewModel.clear()
                                    onOpen(feature)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The heading above a group of entries. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

private const val PREVIEW_WEIGHT = 0.7f
