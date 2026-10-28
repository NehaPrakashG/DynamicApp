package com.example.dynamicform

internal class FieldDefinitionValidator {

    fun validate(
        candidate: FormFieldModel,
        existingFields: List<FormFieldModel>
    ): String? {
        if (candidate.id.isBlank()) {
            return "Field ID cannot be blank."
        }

        if (existingFields.any { it.id == candidate.id }) {
            return "A field with this ID already exists."
        }

        if (candidate.label.isBlank()) {
            return "Field label cannot be blank."
        }

        if (candidate.key.isBlank()) {
            return "Field key cannot be blank."
        }

        // Compare normalized keys to prevent whitespace-only differences.
        val normalizedKey = candidate.key.trim()

        if (existingFields.any { it.key.trim() == normalizedKey }) {
            return "A field with key '$normalizedKey' already exists."
        }

        val requiresOptions =
            candidate.type == FormFieldType.RADIO ||
                    candidate.type == FormFieldType.DROPDOWN

        if (requiresOptions) {
            if (candidate.options.isEmpty()) {
                return "Radio and dropdown fields require at least one option."
            }

            if (candidate.options.any { it.isBlank() }) {
                return "Options cannot be blank."
            }

            val normalizedOptions = candidate.options.map { it.trim() }

            if (normalizedOptions.distinct().size != normalizedOptions.size) {
                return "Options must be unique."
            }
        }

        for (rule in candidate.validationRules) {
            when (rule) {
                is ValidationRule.Required -> Unit

                is ValidationRule.MinLength -> {
                    if (rule.length < 0) {
                        return "Minimum length cannot be negative."
                    }
                }

                is ValidationRule.Regex -> {
                    // Reject invalid patterns before field-value validation.
                    try {
                        Regex(rule.pattern)
                    } catch (_: IllegalArgumentException) {
                        return "Field contains an invalid regex pattern."
                    }
                }
            }
        }

        return null
    }
}