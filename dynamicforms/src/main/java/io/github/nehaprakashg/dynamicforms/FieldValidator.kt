package io.github.nehaprakashg.dynamicforms

/** Validates one field's current value against its definition. */
internal object FieldValidator {

    const val REQUIRED_MESSAGE: String = "This field is required"

    private val EmailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /** Returns the first error for this field, or null if it is valid. */
    fun validate(field: FieldDefinition, state: FieldState): String? {
        if (field.type == FieldType.UNKNOWN) return null

        val text = state.text

        for (rule in field.rules) {
            val error = when (rule) {
                Rule.Required -> when {
                    field.type == FieldType.CHECKBOX -> if (!state.checked) REQUIRED_MESSAGE else null
                    text.isBlank() -> REQUIRED_MESSAGE
                    else -> null
                }

                is Rule.MinLength ->
                    if (text.isNotBlank() && text.length < rule.length) {
                        "Minimum length is ${rule.length} characters"
                    } else null

                is Rule.MaxLength ->
                    if (text.length > rule.length) {
                        "Maximum length is ${rule.length} characters"
                    } else null

                is Rule.Pattern ->
                    if (text.isNotBlank() && !rule.regex.matches(text)) rule.message else null
            }
            if (error != null) return error
        }

        // Built-in checks implied by the field type, applied only to non-empty values.
        if (text.isNotBlank()) {
            when (field.type) {
                FieldType.NUMBER ->
                    if (text.trim().toDoubleOrNull() == null) return "Enter a valid number"

                FieldType.EMAIL ->
                    if (!EmailRegex.matches(text.trim())) return "Enter a valid email address"

                FieldType.DROPDOWN, FieldType.RADIO ->
                    if (text !in field.options) return "Select one of the available options"

                else -> Unit
            }
        }

        return null
    }
}
