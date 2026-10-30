package com.example.dynamicform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFormScreen(
    viewModel: DynamicFormViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dynamic Form Engine") },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Field")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (uiState.formError != null) {
                        Text(
                            text = uiState.formError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onEvent(FormUserEvent.OnResetForm) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset")
                        }
                        Button(
                            onClick = { viewModel.onEvent(FormUserEvent.OnSubmitForm) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Submit")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.fields.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No fields in form. Click '+' to add fields.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(uiState.fields, key = { _, field -> field.id }) { index, field ->
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(12.dp)) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Field [${field.type}]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row {
                                            if (index > 0) {
                                                TextButton(onClick = {
                                                    viewModel.onEvent(FormUserEvent.OnReorderFields(index, index - 1))
                                                }) {
                                                    Text("↑")
                                                }
                                            }
                                            if (index < uiState.fields.size - 1) {
                                                TextButton(onClick = {
                                                    viewModel.onEvent(FormUserEvent.OnReorderFields(index, index + 1))
                                                }) {
                                                    Text("↓")
                                                }
                                            }
                                            IconButton(
                                                onClick = { viewModel.onEvent(FormUserEvent.OnRemoveField(field.id)) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Remove field",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    DynamicFieldRenderer(
                                        field = field,
                                        onValueChange = { newValue ->
                                            viewModel.onEvent(FormUserEvent.OnFieldValueChanged(field.id, newValue))
                                        },
                                        onCheckboxChange = { isChecked ->
                                            viewModel.onEvent(FormUserEvent.OnCheckboxToggled(field.id, isChecked))
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            if (uiState.isAddFieldDialogVisible) {
                AddFieldDialog(
                    errorMessage = uiState.addFieldError,
                    onDismiss = { viewModel.onEvent(FormUserEvent.OnAddFieldDialogDismissed) },
                    onInputChanged = { viewModel.onEvent(FormUserEvent.OnAddFieldInputChanged) },
                    // The ViewModel closes the dialog only if the field is accepted.
                    onAddField = { newField ->
                        viewModel.onEvent(FormUserEvent.OnAddField(newField))
                    }
                )
            }

            // Read the result directly from ViewModel state.
            val submissionResult = uiState.submissionResult

            if (uiState.isSuccess && submissionResult != null) {
                AlertDialog(
                    onDismissRequest = {
                        // Notify the owner of the state when the dialog is dismissed.
                        viewModel.onEvent(
                            FormUserEvent.OnSubmissionResultDismissed
                        )
                    },
                    title = {
                        Text("Form Submitted Successfully!")
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(
                                rememberScrollState()
                            )
                        ) {
                            Text("Validated Form Data:")

                            Spacer(modifier = Modifier.height(8.dp))

                            submissionResult.forEach { (key, value) ->
                                Text(
                                    text = "$key: $value",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                // Use the same dismissal path for the OK button.
                                viewModel.onEvent(
                                    FormUserEvent.OnSubmissionResultDismissed
                                )
                            }
                        ) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AddFieldDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onInputChanged: () -> Unit,
    onAddField: (FormFieldModel) -> Unit
) {
    // Preserve the label across activity recreation.
    var label by rememberSaveable {
        mutableStateOf("")
    }
/***
 * null means "generate the key automatically from the label".
 * Once the user edits the key, store their manual value instead.
 * An empty string is also a manual value; it should not trigger regeneration.
 ***/

    //Derive the automatic key instead of storing a second mutable value.
    var manualKey by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val fieldKey = manualKey ?: generateFieldKey(label)
    // Preserve the remaining inputs across activity recreation as well.
    var selectedType by rememberSaveable { mutableStateOf(FormFieldType.TEXT) }
    var placeholder by rememberSaveable { mutableStateOf("") }
    var isRequired by rememberSaveable { mutableStateOf(false) }
    var optionsText by rememberSaveable { mutableStateOf("") }
    var expandedType by remember { mutableStateOf(false) }

    val showsPlaceholder = selectedType != FormFieldType.CHECKBOX && selectedType != FormFieldType.RADIO && selectedType != FormFieldType.DROPDOWN
    val showsOptions = selectedType == FormFieldType.DROPDOWN || selectedType == FormFieldType.RADIO

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Dynamic Field") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        onInputChanged()
                    },
                    label = { Text("Field Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fieldKey,
                    onValueChange = {
                        manualKey = it
                        onInputChanged()
                    },
                    label = { Text("Field Key (Data Map Key)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Type selector dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedType = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Type: ${selectedType.name}")
                    }
                    DropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        FormFieldType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    expandedType = false
                                    onInputChanged()
                                }
                            )
                        }
                    }
                }

                if (showsPlaceholder) {
                    OutlinedTextField(
                        value = placeholder,
                        onValueChange = {
                            placeholder = it
                            onInputChanged()
                        },
                        label = { Text("Placeholder") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (showsOptions) {
                    OutlinedTextField(
                        value = optionsText,
                        onValueChange = {
                            optionsText = it
                            onInputChanged()
                        },
                        label = { Text("Options (comma-separated)") },
                        placeholder = { Text("Option 1, Option 2, Option 3") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isRequired,
                        onCheckedChange = {
                            isRequired = it
                            onInputChanged()
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Required Field")
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Always submit so the ViewModel can report blank or invalid input in the dialog.
                    val rules = mutableListOf<ValidationRule>()
                    if (isRequired) {
                        rules.add(ValidationRule.Required)
                    }
                    if (selectedType == FormFieldType.EMAIL) {
                        rules.add(ValidationRule.Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", "Invalid email format"))
                    }
                    // Pass options raw; the ViewModel normalizes and the validator rejects blank ones.
                    val optionsList = if (showsOptions && optionsText.isNotBlank()) {
                        optionsText.split(",")
                    } else {
                        emptyList()
                    }

                    val newField = FormFieldModel(
                        id = "field_" + UUID.randomUUID().toString().take(8),
                        key = fieldKey,
                        label = label,
                        type = selectedType,
                        // Drop a placeholder typed for a type whose input is now hidden.
                        placeholder = if (showsPlaceholder) placeholder else "",
                        options = optionsList,
                        validationRules = rules
                    )
                    onAddField(newField)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private val WhitespaceRegex = "\\s+".toRegex()

/***
 * Convert a label such as " First Name " into "first_name".
 * This formats the key; uniqueness and allowed-character validation are separate concerns.
 ***/
private fun generateFieldKey(label: String): String {
    return label
        .trim()
        .lowercase()
        .replace(WhitespaceRegex, "_")
}