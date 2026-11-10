package io.github.nehaprakashg.dynamicforms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormStateTest {

    private val schema = FormSchema(
        id = "test",
        fields = listOf(
            FieldDefinition("name", "Name", FieldType.TEXT, rules = listOf(Rule.Required, Rule.MinLength(2))),
            FieldDefinition("terms", "Terms", FieldType.CHECKBOX, rules = listOf(Rule.Required)),
            FieldDefinition("future", "Future", FieldType.UNKNOWN, rules = listOf(Rule.Required)),
        ),
    )

    @Test
    fun `no errors are shown while the user is still typing`() {
        val state = FormState.initial(schema).withText("name", "N")
        assertNull(state.field("name").error)
        assertFalse(state.isValid)
    }

    @Test
    fun `validateAll shows errors for every invalid field`() {
        val state = FormState.initial(schema).validateAll()
        assertTrue(state.submitAttempted)
        assertEquals(FieldValidator.REQUIRED_MESSAGE, state.field("name").error)
        assertEquals(FieldValidator.REQUIRED_MESSAGE, state.field("terms").error)
        assertNull(state.field("future").error)
    }

    @Test
    fun `after a submit attempt, edits re-validate immediately`() {
        val state = FormState.initial(schema).validateAll()
            .withText("name", "N")
        assertEquals("Minimum length is 2 characters", state.field("name").error)

        val fixed = state.withText("name", "Neha")
        assertNull(fixed.field("name").error)
    }

    @Test
    fun `values are typed, ordered by schema, and skip unknown fields`() {
        val state = FormState.initial(schema)
            .withText("name", "Neha")
            .withChecked("terms", true)

        assertTrue(state.isValid)
        assertEquals(listOf("name", "terms"), state.values().keys.toList())
        assertEquals("Neha", state.values()["name"])
        assertEquals(true, state.values()["terms"])
    }

    @Test
    fun `updates to unknown keys are ignored`() {
        val initial = FormState.initial(schema)
        assertEquals(initial, initial.withText("missing", "x"))
    }

    @Test
    fun `reset clears values, errors and submit state`() {
        val dirty = FormState.initial(schema).withText("name", "Neha").validateAll()
        assertEquals(FormState.initial(schema), dirty.reset())
    }
}
