package io.github.nehaprakashg.dynamicforms.internal

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.nehaprakashg.dynamicforms.FieldDefinition
import io.github.nehaprakashg.dynamicforms.FieldState
import io.github.nehaprakashg.dynamicforms.FieldType
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DynamicFieldRenderer(
    definition: FieldDefinition,
    state: FieldState,
    onTextChange: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val displayLabel = definition.label + if (definition.isRequired) " *" else ""

        val hasError = state.error != null

        if (
            definition.type != FieldType.CHECKBOX &&
            definition.type != FieldType.RADIO
        ) {
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        when (definition.type) {
            FieldType.TEXT, FieldType.EMAIL, FieldType.NUMBER -> {
                val keyboardType = when (definition.type) {
                    FieldType.EMAIL -> KeyboardType.Email
                    FieldType.NUMBER -> KeyboardType.Number
                    else -> KeyboardType.Text
                }
                OutlinedTextField(
                    value = state.text,
                    onValueChange = onTextChange,
                    placeholder = { Text(definition.placeholder) },
                    singleLine = true,
                    isError = hasError,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FieldType.TEXTAREA -> {
                OutlinedTextField(
                    value = state.text,
                    onValueChange = onTextChange,
                    placeholder = { Text(definition.placeholder) },
                    minLines = 3,
                    maxLines = 6,
                    isError = hasError,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FieldType.DROPDOWN -> {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = state.text,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text(definition.placeholder.ifEmpty { "Select ${definition.label}" }) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        isError = hasError,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        definition.options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onTextChange(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            FieldType.CHECKBOX -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()

                        // The row is the single interactive checkbox control.
                        // Expose its current value and checkbox role to accessibility.
                        .toggleable(
                            value = state.checked,
                            role = Role.Checkbox,
                            onValueChange = onCheckedChange
                        )

                        // Provide a sufficiently large interaction area even though
                        // the child checkbox no longer handles interaction itself.
                        .heightIn(min = 48.dp)
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = state.checked,

                        // The parent row handles input; this is the visual indicator.
                        onCheckedChange = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (hasError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            FieldType.RADIO -> {
                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()

                        // Identify these options as one selection group.
                        .selectableGroup()
                ) {
                    definition.options.forEach { option ->
                        val isSelected = state.text == option

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()

                                // The entire row represents one radio option.
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onTextChange(option)
                                    }
                                )
                                .heightIn(min = 48.dp)
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,

                                // Delegate interaction to the selectable row.
                                onClick = null
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            FieldType.DATE_PICKER -> {
                val context = LocalContext.current
                val showPicker = {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            onTextChange(
                                String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            )
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
                OutlinedTextField(
                    value = state.text,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(definition.placeholder) },
                    trailingIcon = {
                        IconButton(onClick = showPicker) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Select ${definition.label}"
                            )
                        }
                    },
                    isError = hasError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = showPicker)
                )
            }

            // Types this library version doesn't know are filtered out before rendering.
            FieldType.UNKNOWN -> Unit
        }

        if (state.error != null) {
            Text(
                text = state.error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
