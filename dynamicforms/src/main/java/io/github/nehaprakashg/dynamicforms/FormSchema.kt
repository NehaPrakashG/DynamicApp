package io.github.nehaprakashg.dynamicforms

/**
 * A complete form definition, typically delivered by a backend as JSON.
 *
 * The schema is immutable and contains no user input. Runtime values and
 * errors live in [FormState] / [DynamicFormState].
 *
 * Parse one with [FormSchema.Companion.fromJson].
 */
public data class FormSchema(
    val id: String,
    val version: Int = 1,
    val title: String? = null,
    val fields: List<FieldDefinition>,
) {
    public companion object
}

/** The definition of one field: what it is, how it looks, and how it is validated. */
public data class FieldDefinition(
    val key: String,
    val label: String,
    val type: FieldType,
    val placeholder: String = "",
    val options: List<String> = emptyList(),
    val rules: List<Rule> = emptyList(),
) {
    public val isRequired: Boolean
        get() = rules.any { it is Rule.Required }
}

/**
 * Supported field types. In JSON they are written in lowercase,
 * e.g. `"text"` or `"date_picker"` (matching is case-insensitive).
 */
public enum class FieldType {
    TEXT,
    NUMBER,
    EMAIL,
    TEXTAREA,
    DROPDOWN,
    CHECKBOX,
    RADIO,
    DATE_PICKER,

    /**
     * A type this version of the library does not recognize, e.g. one a newer
     * backend introduced. It is skipped: not rendered, validated or submitted.
     * This keeps old app versions working when the server adds field types.
     */
    UNKNOWN;

    internal val requiresOptions: Boolean
        get() = this == DROPDOWN || this == RADIO

    internal companion object {
        fun fromWire(value: String): FieldType =
            entries.firstOrNull { it != UNKNOWN && it.name.equals(value.trim(), ignoreCase = true) }
                ?: UNKNOWN
    }
}

/** A validation rule attached to a field. */
public sealed interface Rule {
    /** The field must have a value (or be checked, for a checkbox). */
    public data object Required : Rule

    /** A non-empty value must be at least [length] characters. */
    public data class MinLength(val length: Int) : Rule

    /** The value must be at most [length] characters. */
    public data class MaxLength(val length: Int) : Rule

    /** A non-empty value must fully match [pattern]; otherwise [message] is shown. */
    public data class Pattern(val pattern: String, val message: String = "Invalid format") : Rule {
        // Compiled once per rule, not on every keystroke.
        internal val regex: Regex by lazy { Regex(pattern) }
    }
}

/** Fields the library knows how to render; [FieldType.UNKNOWN] fields are skipped. */
internal val FormSchema.renderableFields: List<FieldDefinition>
    get() = fields.filter { it.type != FieldType.UNKNOWN }
