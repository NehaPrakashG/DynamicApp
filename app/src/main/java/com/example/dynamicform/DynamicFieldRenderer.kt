package com.example.dynamicform

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFieldRenderer(
    field: FormFieldModel,
    onValueChange: (String) -> Unit,
    onCheckboxChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        if (field.type != FormFieldType.CHECKBOX && field.type != FormFieldType.RADIO) {
            Text(
                text = field.label + if (field.validationRules.any { it is ValidationRule.Required }) " *" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = if (field.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        when (field.type) {
            FormFieldType.TEXT, FormFieldType.EMAIL, FormFieldType.NUMBER -> {
                val keyboardType = when (field.type) {
                    FormFieldType.EMAIL -> KeyboardType.Email
                    FormFieldType.NUMBER -> KeyboardType.Number
                    else -> KeyboardType.Text
                }
                OutlinedTextField(
                    value = field.currentValue,
                    onValueChange = onValueChange,
                    placeholder = { Text(field.placeholder) },
                    singleLine = true,
                    isError = field.errorMessage != null,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FormFieldType.TEXTAREA -> {
                OutlinedTextField(
                    value = field.currentValue,
                    onValueChange = onValueChange,
                    placeholder = { Text(field.placeholder) },
                    minLines = 3,
                    maxLines = 6,
                    isError = field.errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FormFieldType.DROPDOWN -> {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = field.currentValue,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text(field.placeholder.ifEmpty { "Select ${field.label}" }) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        isError = field.errorMessage != null,
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        field.options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onValueChange(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            FormFieldType.CHECKBOX -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCheckboxChange(!field.isChecked) }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = field.isChecked,
                        onCheckedChange = onCheckboxChange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = field.label + if (field.validationRules.any { it is ValidationRule.Required }) " *" else "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (field.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            FormFieldType.RADIO -> {
                Text(
                    text = field.label + if (field.validationRules.any { it is ValidationRule.Required }) " *" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (field.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    field.options.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onValueChange(option) }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = field.currentValue == option,
                                onClick = { onValueChange(option) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = option, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            FormFieldType.DATE_PICKER -> {
                val context = LocalContext.current
                val calendar = Calendar.getInstance()
                OutlinedTextField(
                    value = field.currentValue,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(field.placeholder) },
                    trailingIcon = {
                        IconButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    onValueChange(formattedDate)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Select Date"
                            )
                        }
                    },
                    isError = field.errorMessage != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    onValueChange(formattedDate)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                )
            }
        }

        if (field.errorMessage != null) {
            Text(
                text = field.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
