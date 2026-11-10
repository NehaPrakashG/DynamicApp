package com.example.dynamicapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.nehaprakashg.dynamicforms.DynamicForm
import io.github.nehaprakashg.dynamicforms.FormSchema
import io.github.nehaprakashg.dynamicforms.InvalidSchemaException
import io.github.nehaprakashg.dynamicforms.fromJson
import io.github.nehaprakashg.dynamicforms.rememberDynamicFormState

/**
 * Sample app for the DynamicForms library.
 *
 * The two form tabs load JSON from assets/forms, standing in for a backend response.
 * The playground tab re-renders the form live as you edit its JSON.
 */
private enum class SampleTab(val title: String, val asset: String?) {
    CONTACT("Contact", "forms/contact.json"),
    INSURANCE("Insurance", "forms/insurance_quote.json"),
    PLAYGROUND("Playground", null),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleScreen(modifier: Modifier = Modifier) {
    var selectedTab by rememberSaveable { mutableStateOf(SampleTab.CONTACT) }
    var submitted by remember { mutableStateOf<Map<String, Any>?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("DynamicForms") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                    SampleTab.entries.forEach { tab ->
                        Tab(
                            selected = tab == selectedTab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.title) },
                        )
                    }
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (selectedTab) {
            SampleTab.PLAYGROUND -> PlaygroundTab(
                onSubmit = { submitted = it },
                modifier = contentModifier,
            )

            else -> AssetFormTab(
                assetPath = requireNotNull(selectedTab.asset),
                onSubmit = { submitted = it },
                modifier = contentModifier,
            )
        }
    }

    submitted?.let { values ->
        SubmissionDialog(values = values, onDismiss = { submitted = null })
    }
}

/** Renders a schema loaded from assets, the way an app would render one fetched from a server. */
@Composable
private fun AssetFormTab(
    assetPath: String,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val schemaResult = remember(assetPath) {
        runCatching {
            val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            FormSchema.fromJson(json)
        }
    }

    schemaResult.fold(
        onSuccess = { schema ->
            // Hoisted so the Reset button outside the form can control it.
            val formState = rememberDynamicFormState(schema)

            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                schema.title?.let {
                    Text(it, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Schema '${schema.id}' v${schema.version}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                DynamicForm(state = formState, onSubmit = onSubmit, submitLabel = null)

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = formState::reset, modifier = Modifier.weight(1f)) {
                        Text("Reset")
                    }
                    Button(
                        onClick = { if (formState.validate()) onSubmit(formState.values()) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Submit")
                    }
                }
            }
        },
        onFailure = { error -> SchemaErrors(error, modifier.padding(16.dp)) },
    )
}

/** Edit the schema JSON and watch the form re-render. */
@Composable
private fun PlaygroundTab(
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var json by rememberSaveable { mutableStateOf(PLAYGROUND_STARTER) }
    val parsed = remember(json) { runCatching { FormSchema.fromJson(json) } }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Schema JSON", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = json,
            onValueChange = { json = it },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp),
        )

        Text("Preview", style = MaterialTheme.typography.titleMedium)
        parsed.fold(
            onSuccess = { schema -> DynamicForm(schema = schema, onSubmit = onSubmit) },
            onFailure = { error -> SchemaErrors(error) },
        )
    }
}

@Composable
private fun SchemaErrors(error: Throwable, modifier: Modifier = Modifier) {
    val problems = (error as? InvalidSchemaException)?.problems
        ?: listOf(error.message ?: error::class.java.simpleName)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Invalid schema",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
        )
        problems.forEach {
            Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SubmissionDialog(values: Map<String, Any>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Form submitted") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                values.forEach { (key, value) ->
                    Text("$key: $value", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("OK") }
        },
    )
}

private val PLAYGROUND_STARTER = """
{
  "id": "playground",
  "fields": [
    { "key": "name", "label": "Name", "type": "text",
      "rules": [{ "type": "required" }] },
    { "key": "plan", "label": "Plan", "type": "radio",
      "options": ["Basic", "Plus", "Premium"] },
    { "key": "newsletter", "label": "Send me updates", "type": "checkbox" }
  ]
}
""".trimIndent()
