/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** A dialog that offers [choices] as radio buttons, each shown by its [label]; picking one closes it. */
@Composable
fun <T> ChoiceDialog(
    title: String,
    choices: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onChoose: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        title = { Text(title) },
        text = {
            Column {
                choices.forEach { choice ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .selectable(selected = choice == selected, role = Role.RadioButton) { onChoose(choice) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = choice == selected, onClick = null)
                        Text(label(choice), modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
    )
}
