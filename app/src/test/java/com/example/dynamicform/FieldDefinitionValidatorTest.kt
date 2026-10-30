package com.example.dynamicform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FieldDefinitionValidatorTest {

    private val validator = FieldDefinitionValidator()

    private val existing = listOf(
        FormFieldModel(id = "field_1", key = "full_name", label = "Full Name", type = FormFieldType.TEXT)
    )

    private fun candidate(
        id: String = "field_new",
        key: String = "nickname",
        label: String = "Nickname",
        type: FormFieldType = FormFieldType.TEXT,
        options: List<String> = emptyList(),
        rules: List<ValidationRule> = emptyList()
    ) = FormFieldModel(
        id = id,
        key = key,
        label = label,
        type = type,
        options = options,
        validationRules = rules
    )

    @Test
    fun validCandidate_returnsNull() {
        assertNull(validator.validate(candidate(), existing))
    }

    @Test
    fun blankId_isRejected() {
        assertEquals("Field ID cannot be blank.", validator.validate(candidate(id = " "), existing))
    }

    @Test
    fun duplicateId_isRejected() {
        assertEquals(
            "A field with this ID already exists.",
            validator.validate(candidate(id = "field_1"), existing)
        )
    }

    @Test
    fun blankLabel_isRejected() {
        assertEquals("Field label cannot be blank.", validator.validate(candidate(label = "  "), existing))
    }

    @Test
    fun blankKey_isRejected() {
        assertEquals("Field key cannot be blank.", validator.validate(candidate(key = ""), existing))
    }

    @Test
    fun duplicateKey_isRejected_ignoringSurroundingWhitespace() {
        assertEquals(
            "A field with key 'full_name' already exists.",
            validator.validate(candidate(key = " full_name "), existing)
        )
    }

    @Test
    fun selectionFieldWithoutOptions_isRejected() {
        assertEquals(
            "Radio and dropdown fields require at least one option.",
            validator.validate(candidate(type = FormFieldType.DROPDOWN), existing)
        )
    }

    @Test
    fun blankOption_isRejected() {
        assertEquals(
            "Options cannot be blank.",
            validator.validate(candidate(type = FormFieldType.RADIO, options = listOf("A", " ")), existing)
        )
    }

    @Test
    fun duplicateOptions_areRejected() {
        assertEquals(
            "Options must be unique.",
            validator.validate(candidate(type = FormFieldType.RADIO, options = listOf("A", " A")), existing)
        )
    }

    @Test
    fun nonSelectionField_ignoresOptions() {
        assertNull(validator.validate(candidate(type = FormFieldType.TEXT, options = listOf(" ")), existing))
    }

    @Test
    fun negativeMinLength_isRejected() {
        assertEquals(
            "Minimum length cannot be negative.",
            validator.validate(candidate(rules = listOf(ValidationRule.MinLength(-1))), existing)
        )
    }

    @Test
    fun invalidRegex_isRejected() {
        assertEquals(
            "Field contains an invalid regex pattern.",
            validator.validate(candidate(rules = listOf(ValidationRule.Regex("[unclosed"))), existing)
        )
    }
}
