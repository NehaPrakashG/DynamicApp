package io.github.nehaprakashg.dynamicforms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FieldValidatorTest {

    private fun field(type: FieldType, vararg rules: Rule, options: List<String> = emptyList()) =
        FieldDefinition(key = "k", label = "Label", type = type, options = options, rules = rules.toList())

    private fun validate(definition: FieldDefinition, text: String = "", checked: Boolean = false) =
        FieldValidator.validate(definition, FieldState(text = text, checked = checked))

    @Test
    fun `required text field fails when blank`() {
        assertEquals(FieldValidator.REQUIRED_MESSAGE, validate(field(FieldType.TEXT, Rule.Required), "   "))
    }

    @Test
    fun `required checkbox fails until checked`() {
        val terms = field(FieldType.CHECKBOX, Rule.Required)
        assertEquals(FieldValidator.REQUIRED_MESSAGE, validate(terms, checked = false))
        assertNull(validate(terms, checked = true))
    }

    @Test
    fun `optional field with min length accepts empty value`() {
        assertNull(validate(field(FieldType.TEXTAREA, Rule.MinLength(10)), ""))
    }

    @Test
    fun `min and max length are enforced`() {
        val bio = field(FieldType.TEXTAREA, Rule.MinLength(3), Rule.MaxLength(5))
        assertEquals("Minimum length is 3 characters", validate(bio, "ab"))
        assertEquals("Maximum length is 5 characters", validate(bio, "abcdef"))
        assertNull(validate(bio, "abcd"))
    }

    @Test
    fun `pattern rule shows its own message`() {
        val zip = field(FieldType.TEXT, Rule.Pattern("^\\d{5}$", "Enter a 5-digit ZIP code"))
        assertEquals("Enter a 5-digit ZIP code", validate(zip, "1234"))
        assertNull(validate(zip, "60601"))
    }

    @Test
    fun `first failing rule wins`() {
        val name = field(FieldType.TEXT, Rule.Required, Rule.MinLength(2))
        assertEquals(FieldValidator.REQUIRED_MESSAGE, validate(name, ""))
        assertEquals("Minimum length is 2 characters", validate(name, "a"))
    }

    @Test
    fun `number and email types are format-checked`() {
        assertEquals("Enter a valid number", validate(field(FieldType.NUMBER), "12a"))
        assertNull(validate(field(FieldType.NUMBER), "42"))
        assertEquals("Enter a valid email address", validate(field(FieldType.EMAIL), "neha@"))
        assertNull(validate(field(FieldType.EMAIL), "neha@example.com"))
    }

    @Test
    fun `dropdown rejects values that are not options`() {
        val country = field(FieldType.DROPDOWN, options = listOf("Canada", "India"))
        assertEquals("Select one of the available options", validate(country, "Mars"))
        assertNull(validate(country, "India"))
    }

    @Test
    fun `unknown field types are never invalid`() {
        assertNull(validate(field(FieldType.UNKNOWN, Rule.Required), ""))
    }
}
