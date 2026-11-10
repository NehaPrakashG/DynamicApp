package io.github.nehaprakashg.dynamicforms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.nehaprakashg.dynamicforms.internal.DynamicFieldRenderer

/**
 * Renders a form from [schema] and calls [onSubmit] with the values once every field is valid.
 *
 * The form does not scroll by itself; add `Modifier.verticalScroll(...)` if it may be long.
 */
@Composable
public fun DynamicForm(
    schema: FormSchema,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier,
    submitLabel: String? = "Submit",
) {
    DynamicForm(
        state = rememberDynamicFormState(schema),
        onSubmit = onSubmit,
        modifier = modifier,
        submitLabel = submitLabel,
    )
}

/**
 * Renders the form held by [state]. Use this overload to control the form from outside,
 * e.g. to validate or reset it from your own buttons. Pass `submitLabel = null` to hide
 * the built-in submit button.
 */
@Composable
public fun DynamicForm(
    state: DynamicFormState,
    onSubmit: (Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier,
    submitLabel: String? = "Submit",
) {
    val form = state.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        form.schema.renderableFields.forEach { definition ->
            key(definition.key) {
                DynamicFieldRenderer(
                    definition = definition,
                    state = form.field(definition.key),
                    onTextChange = { state.onTextChange(definition.key, it) },
                    onCheckedChange = { state.onCheckedChange(definition.key, it) },
                )
            }
        }

        if (submitLabel != null) {
            Button(
                onClick = { if (state.validate()) onSubmit(state.values()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(submitLabel)
            }
        }
    }
}
