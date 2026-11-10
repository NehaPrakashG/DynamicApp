package io.github.nehaprakashg.dynamicforms

/** The user's input for one field, plus the error currently shown for it. */
public data class FieldState(
    val text: String = "",
    val checked: Boolean = false,
    val error: String? = null,
)

/**
 * An immutable snapshot of a form: its schema plus the state of every field.
 *
 * Pure Kotlin with no Android or Compose dependency, so all form logic is
 * unit-testable on the JVM. [DynamicFormState] wraps it for Compose.
 */
public data class FormState(
    val schema: FormSchema,
    val fields: Map<String, FieldState>,
    /** Once true, every edit is re-validated immediately. */
    val submitAttempted: Boolean = false,
) {
    /** The state of the field with [key], or an empty state if there is none. */
    public fun field(key: String): FieldState = fields[key] ?: FieldState()

    /** True if every renderable field currently passes validation. */
    public val isValid: Boolean
        get() = schema.renderableFields.all { FieldValidator.validate(it, field(it.key)) == null }

    /**
     * The submitted values keyed by field key, in schema order:
     * a [Boolean] for checkboxes and a [String] for everything else.
     * Unknown field types are left out.
     */
    public fun values(): Map<String, Any> = buildMap {
        schema.renderableFields.forEach { definition ->
            val state = field(definition.key)
            put(definition.key, if (definition.type == FieldType.CHECKBOX) state.checked else state.text)
        }
    }

    public companion object {
        /** An empty form for [schema]. */
        public fun initial(schema: FormSchema): FormState =
            FormState(schema = schema, fields = schema.fields.associate { it.key to FieldState() })
    }
}

// ---- Reducers: every state change goes through one of these pure functions. ----

internal fun FormState.withText(key: String, text: String): FormState =
    updateField(key) { it.copy(text = text) }

internal fun FormState.withChecked(key: String, checked: Boolean): FormState =
    updateField(key) { it.copy(checked = checked) }

/** Validates every field and marks the form as submitted, so errors show from now on. */
internal fun FormState.validateAll(): FormState {
    val definitions = schema.fields.associateBy { it.key }
    return copy(
        submitAttempted = true,
        fields = fields.mapValues { (key, state) ->
            val definition = definitions[key] ?: return@mapValues state
            state.copy(error = FieldValidator.validate(definition, state))
        },
    )
}

internal fun FormState.reset(): FormState = FormState.initial(schema)

private fun FormState.updateField(key: String, transform: (FieldState) -> FieldState): FormState {
    val definition = schema.fields.firstOrNull { it.key == key } ?: return this
    val current = field(key)
    val next = transform(current)

    // Don't flag a field while the user is still filling it in for the first time.
    // Once it has shown an error, or the user has tried to submit, re-check on every edit
    // so the message clears as soon as the input becomes valid.
    val shouldValidate = submitAttempted || current.error != null
    val error = if (shouldValidate) FieldValidator.validate(definition, next) else null

    return copy(fields = fields + (key to next.copy(error = error)))
}
