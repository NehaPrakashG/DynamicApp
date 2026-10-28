package com.example.dynamicform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFormScreen(
    viewModel: DynamicFormViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddFieldDialog by remember { mutableStateOf(false) }
    var successDialogMessage by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(uiState.isSuccess, uiState.submissionResult) {
        if (uiState.isSuccess && uiState.submissionResult != null) {
            successDialogMessage = uiState.submissionResult
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dynamic Form Engine") },
                actions = {
                    IconButton(onClick = { showAddFieldDialog = true }) {
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

            if (showAddFieldDialog) {
                AddFieldDialog(
                    onDismiss = { showAddFieldDialog = false },
                    onAddField = { newField ->
                        viewModel.onEvent(FormUserEvent.OnAddField(newField))
                        showAddFieldDialog = false
                    }
                )
            }

            if (successDialogMessage != null) {
                AlertDialog(
                    onDismissRequest = { successDialogMessage = null },
                    title = { Text("Form Submitted Successfully!") },
                    text = {
                        Column {
                            Text("Validated Form Data:")
                            Spacer(modifier = Modifier.height(8.dp))
                            successDialogMessage?.forEach { (key, value) ->
                                Text("$key: $value", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { successDialogMessage = null }) {
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
    onDismiss: () -> Unit,
    onAddField: (FormFieldModel) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(FormFieldType.TEXT) }
    var placeholder by remember { mutableStateOf("") }
    var isRequired by remember { mutableStateOf(false) }
    var optionsText by remember { mutableStateOf("") }
    var expandedType by remember { mutableStateOf(false) }

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
                        if (key.isBlank()) {
                            key = it.lowercase().replace("\\s+".toRegex(), "_")
                        }
                    },
                    label = { Text("Field Label") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Field Key (Data Map Key)") },
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
                                }
                            )
                        }
                    }
                }

                if (selectedType != FormFieldType.CHECKBOX && selectedType != FormFieldType.RADIO && selectedType != FormFieldType.DROPDOWN) {
                    OutlinedTextField(
                        value = placeholder,
                        onValueChange = { placeholder = it },
                        label = { Text("Placeholder") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedType == FormFieldType.DROPDOWN || selectedType == FormFieldType.RADIO) {
                    OutlinedTextField(
                        value = optionsText,
                        onValueChange = { optionsText = it },
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
                        onCheckedChange = { isRequired = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Required Field")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank() && key.isNotBlank()) {
                        val rules = mutableListOf<ValidationRule>()
                        if (isRequired) {
                            rules.add(ValidationRule.Required)
                        }
                        if (selectedType == FormFieldType.EMAIL) {
                            rules.add(ValidationRule.Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", "Invalid email format"))
                        }
                        val optionsList = if (selectedType == FormFieldType.DROPDOWN || selectedType == FormFieldType.RADIO) {
                            optionsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        } else {
                            emptyList()
                        }

                        val newField = FormFieldModel(
                            id = "field_" + UUID.randomUUID().toString().take(8),
                            key = key,
                            label = label,
                            type = selectedType,
                            placeholder = placeholder,
                            options = optionsList,
                            validationRules = rules
                        )
                        onAddField(newField)
                    }
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
