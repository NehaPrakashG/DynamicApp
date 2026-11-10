package io.github.nehaprakashg.dynamicforms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Compose state holder for a dynamic form.
 *
 * Create one with [rememberDynamicFormState]. Hoist it when the host screen
 * needs to read values, validate, or reset the form from outside [DynamicForm].
 * Hosting it in a ViewModel is optional and up to the app.
 */
@Stable
public class DynamicFormState internal constructor(initial: FormState) {

    /** The current immutable snapshot of the form. */
    public var current: FormState by mutableStateOf(initial)
        private set

    public val schema: FormSchema
        get() = current.schema

    public fun onTextChange(key: String, text: String) {
        current = current.withText(key, text)
    }

    public fun onCheckedChange(key: String, checked: Boolean) {
        current = current.withChecked(key, checked)
    }

    /** Validates every field and shows any errors. Returns true if the form is valid. */
    public fun validate(): Boolean {
        current = current.validateAll()
        return current.fields.values.none { it.error != null }
    }

    /** See [FormState.values]. */
    public fun values(): Map<String, Any> = current.values()

    public fun reset() {
        current = current.reset()
    }

    internal companion object {
        /** Survives rotation and process death: saves only the raw input, then re-derives errors. */
        fun saver(schema: FormSchema): Saver<DynamicFormState, Any> = listSaver(
            save = { state ->
                val snapshot = state.current
                val keys = snapshot.fields.keys.toList()
                listOf(
                    ArrayList(keys),
                    ArrayList(keys.map { snapshot.field(it).text }),
                    keys.map { snapshot.field(it).checked }.toBooleanArray(),
                    snapshot.submitAttempted,
                )
            },
            restore = { saved ->
                @Suppress("UNCHECKED_CAST")
                val keys = saved[0] as List<String>
                @Suppress("UNCHECKED_CAST")
                val texts = saved[1] as List<String>
                val checked = saved[2] as BooleanArray
                val submitAttempted = saved[3] as Boolean

                var restored = FormState.initial(schema)
                keys.forEachIndexed { i, key ->
                    restored = restored.withText(key, texts[i]).withChecked(key, checked[i])
                }
                DynamicFormState(if (submitAttempted) restored.validateAll() else restored)
            },
        )
    }
}

/** Remembers a [DynamicFormState] for [schema]. A new schema starts a fresh form. */
@Composable
public fun rememberDynamicFormState(schema: FormSchema): DynamicFormState =
    rememberSaveable(schema, saver = DynamicFormState.saver(schema)) {
        DynamicFormState(FormState.initial(schema))
    }
