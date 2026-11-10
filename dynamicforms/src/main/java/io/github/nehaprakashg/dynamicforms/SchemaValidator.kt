package io.github.nehaprakashg.dynamicforms

/**
 * Checks that a schema is internally consistent before it is rendered:
 * unique keys, options where required, sane rule parameters, valid regexes.
 *
 * Returns every problem found rather than stopping at the first one, so a
 * backend developer can fix a bad schema in one pass.
 */
internal object SchemaValidator {

    fun validate(schema: FormSchema): List<String> {
        val problems = mutableListOf<String>()

        if (schema.id.isBlank()) {
            problems += "Schema id cannot be blank."
        }

        val seenKeys = mutableSetOf<String>()

        schema.fields.forEachIndexed { index, field ->
            val where = "fields[$index]" + if (field.key.isNotBlank()) " ('${field.key}')" else ""

            if (field.key.isBlank()) {
                problems += "$where: key cannot be blank."
            } else if (!seenKeys.add(field.key)) {
                problems += "$where: duplicate key '${field.key}'."
            }

            if (field.label.isBlank()) {
                problems += "$where: label cannot be blank."
            }

            if (field.type.requiresOptions) {
                when {
                    field.options.isEmpty() ->
                        problems += "$where: ${field.type.name.lowercase()} fields need at least one option."

                    field.options.any { it.isBlank() } ->
                        problems += "$where: options cannot be blank."

                    field.options.distinct().size != field.options.size ->
                        problems += "$where: options must be unique."
                }
            }

            field.rules.forEach { rule ->
                when (rule) {
                    Rule.Required -> Unit

                    is Rule.MinLength ->
                        if (rule.length < 0) problems += "$where: min_length cannot be negative."

                    is Rule.MaxLength ->
                        if (rule.length < 0) problems += "$where: max_length cannot be negative."

                    is Rule.Pattern ->
                        // Reject invalid patterns up front instead of crashing while the user types.
                        try {
                            rule.regex
                        } catch (_: IllegalArgumentException) {
                            problems += "$where: invalid regex '${rule.pattern}'."
                        }
                }
            }

            val min = field.rules.filterIsInstance<Rule.MinLength>().maxOfOrNull { it.length }
            val max = field.rules.filterIsInstance<Rule.MaxLength>().minOfOrNull { it.length }
            if (min != null && max != null && min > max) {
                problems += "$where: min_length ($min) is greater than max_length ($max)."
            }
        }

        return problems
    }
}
