package io.github.nehaprakashg.dynamicforms

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Thrown when a schema can't be parsed or is internally inconsistent. */
public class InvalidSchemaException(
    public val problems: List<String>,
    cause: Throwable? = null,
) : IllegalArgumentException(problems.joinToString(separator = "\n"), cause)

/**
 * Parses and validates a form schema from JSON.
 *
 * Forward-compatible by design: unknown JSON properties are ignored, unknown
 * field types become [FieldType.UNKNOWN] and are skipped, and unknown rule
 * types are dropped. A schema written for a newer app version still renders
 * on an older one.
 *
 * @throws InvalidSchemaException if the JSON is malformed or the schema is inconsistent.
 */
public fun FormSchema.Companion.fromJson(json: String): FormSchema {
    val dto = try {
        SchemaJson.decodeFromString(FormSchemaDto.serializer(), json)
    } catch (e: IllegalArgumentException) {
        // SerializationException (malformed JSON, missing required property) is an IllegalArgumentException.
        throw InvalidSchemaException(listOf("Malformed schema JSON: ${e.message}"), e)
    }

    val problems = mutableListOf<String>()
    val schema = dto.toDomain(problems)
    problems += SchemaValidator.validate(schema)

    if (problems.isNotEmpty()) throw InvalidSchemaException(problems)
    return schema
}

private val SchemaJson = Json {
    ignoreUnknownKeys = true
}

// ---- Wire format. Kept internal so the JSON shape can evolve without breaking the public API. ----

@Serializable
internal data class FormSchemaDto(
    val id: String,
    val version: Int = 1,
    val title: String? = null,
    val fields: List<FieldDto> = emptyList(),
)

@Serializable
internal data class FieldDto(
    val key: String,
    val label: String,
    val type: String,
    val placeholder: String = "",
    val options: List<String> = emptyList(),
    val rules: List<RuleDto> = emptyList(),
)

@Serializable
internal data class RuleDto(
    val type: String,
    val length: Int? = null,
    val pattern: String? = null,
    val message: String? = null,
)

private fun FormSchemaDto.toDomain(problems: MutableList<String>): FormSchema =
    FormSchema(
        id = id.trim(),
        version = version,
        title = title,
        fields = fields.mapIndexed { index, field -> field.toDomain(index, problems) },
    )

private fun FieldDto.toDomain(index: Int, problems: MutableList<String>): FieldDefinition =
    FieldDefinition(
        key = key.trim(),
        label = label.trim(),
        type = FieldType.fromWire(type),
        placeholder = placeholder,
        options = options.map { it.trim() },
        rules = rules.mapNotNull { it.toDomain("fields[$index] ('${key.trim()}')", problems) },
    )

private fun RuleDto.toDomain(where: String, problems: MutableList<String>): Rule? =
    when (type.trim().lowercase()) {
        "required" -> Rule.Required

        "min_length" -> length?.let(Rule::MinLength)
            ?: null.also { problems += "$where: min_length rule needs a 'length'." }

        "max_length" -> length?.let(Rule::MaxLength)
            ?: null.also { problems += "$where: max_length rule needs a 'length'." }

        "regex", "pattern" -> pattern?.let { Rule.Pattern(it, message ?: "Invalid format") }
            ?: null.also { problems += "$where: regex rule needs a 'pattern'." }

        // A rule type from a newer schema version: skip it rather than reject the whole form.
        else -> null
    }
