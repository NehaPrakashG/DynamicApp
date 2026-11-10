package io.github.nehaprakashg.dynamicforms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SchemaParserTest {

    private fun parseErrors(json: String): List<String> =
        try {
            FormSchema.fromJson(json)
            fail("Expected InvalidSchemaException")
            emptyList()
        } catch (e: InvalidSchemaException) {
            e.problems
        }

    @Test
    fun `parses fields, options and rules`() {
        val schema = FormSchema.fromJson(
            """
            {
              "id": "contact", "version": 2, "title": "Contact",
              "fields": [
                { "key": "email", "label": "Email", "type": "email",
                  "rules": [{ "type": "required" }, { "type": "regex", "pattern": ".+@.+", "message": "Bad email" }] },
                { "key": "country", "label": "Country", "type": "DROPDOWN", "options": [" India ", "Canada"] }
              ]
            }
            """
        )

        assertEquals("contact", schema.id)
        assertEquals(2, schema.version)
        assertEquals(FieldType.EMAIL, schema.fields[0].type)
        assertEquals(listOf(Rule.Required, Rule.Pattern(".+@.+", "Bad email")), schema.fields[0].rules)
        assertEquals(FieldType.DROPDOWN, schema.fields[1].type) // type matching is case-insensitive
        assertEquals(listOf("India", "Canada"), schema.fields[1].options) // options are trimmed
    }

    @Test
    fun `unknown properties, field types and rule types are tolerated`() {
        val schema = FormSchema.fromJson(
            """
            {
              "id": "future", "layout": "two_column",
              "fields": [
                { "key": "sig", "label": "Signature", "type": "signature_pad", "rules": [{ "type": "required" }] },
                { "key": "name", "label": "Name", "type": "text", "rules": [{ "type": "profanity_filter" }] }
              ]
            }
            """
        )

        assertEquals(FieldType.UNKNOWN, schema.fields[0].type)
        assertTrue(schema.fields[1].rules.isEmpty())
    }

    @Test
    fun `malformed json is reported`() {
        val problems = parseErrors("""{ "id": "x", "fields": [ """)
        assertTrue(problems.single().startsWith("Malformed schema JSON"))
    }

    @Test
    fun `all schema problems are reported together`() {
        val problems = parseErrors(
            """
            {
              "id": "bad",
              "fields": [
                { "key": "a", "label": "A", "type": "text" },
                { "key": "a", "label": "Duplicate", "type": "text" },
                { "key": "plan", "label": "Plan", "type": "radio" },
                { "key": "zip", "label": "ZIP", "type": "text", "rules": [{ "type": "regex", "pattern": "[" }] },
                { "key": "bio", "label": "Bio", "type": "textarea", "rules": [{ "type": "min_length" }] }
              ]
            }
            """
        )

        assertEquals(4, problems.size)
        assertTrue(problems.any { "duplicate key 'a'" in it })
        assertTrue(problems.any { "need at least one option" in it })
        assertTrue(problems.any { "invalid regex" in it })
        assertTrue(problems.any { "min_length rule needs a 'length'" in it })
    }
}
