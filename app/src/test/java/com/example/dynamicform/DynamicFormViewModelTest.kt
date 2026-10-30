package com.example.dynamicform

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DynamicFormViewModelTest {

    private lateinit var viewModel: DynamicFormViewModel

    private val state get() = viewModel.uiState.value

    @Before
    fun setUp() {
        viewModel = DynamicFormViewModel()
    }

    private fun newField(
        id: String = "field_new",
        key: String = "nickname",
        label: String = "Nickname"
    ) = FormFieldModel(id = id, key = key, label = label, type = FormFieldType.TEXT)

    private fun fillAllRequiredFields() {
        state.fields.forEach { field ->
            val event = when (field.type) {
                FormFieldType.CHECKBOX -> FormUserEvent.OnCheckboxToggled(field.id, true)
                FormFieldType.EMAIL -> FormUserEvent.OnFieldValueChanged(field.id, "a@example.com")
                FormFieldType.NUMBER -> FormUserEvent.OnFieldValueChanged(field.id, "30")
                FormFieldType.TEXTAREA -> FormUserEvent.OnFieldValueChanged(field.id, "Long enough biography")
                FormFieldType.RADIO, FormFieldType.DROPDOWN ->
                    FormUserEvent.OnFieldValueChanged(field.id, field.options.first())
                FormFieldType.DATE_PICKER -> FormUserEvent.OnFieldValueChanged(field.id, "2000-01-01")
                FormFieldType.TEXT -> FormUserEvent.OnFieldValueChanged(field.id, "Jane Doe")
            }
            viewModel.onEvent(event)
        }
    }

    private fun submitSuccessfully() {
        fillAllRequiredFields()
        viewModel.onEvent(FormUserEvent.OnSubmitForm)
        assertTrue(state.isSuccess)
        assertNotNull(state.submissionResult)
    }

    // region Add-field dialog

    @Test
    fun openingDialog_showsItWithoutError() {
        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)

        assertTrue(state.isAddFieldDialogVisible)
        assertNull(state.addFieldError)
    }

    @Test
    fun validField_isAddedNormalized_andClosesDialog() {
        val sizeBefore = state.fields.size
        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)

        viewModel.onEvent(FormUserEvent.OnAddField(newField(key = " nickname ", label = " Nickname ")))

        assertEquals(sizeBefore + 1, state.fields.size)
        val added = state.fields.last()
        assertEquals("nickname", added.key)
        assertEquals("Nickname", added.label)
        assertFalse(state.isAddFieldDialogVisible)
        assertNull(state.addFieldError)
    }

    @Test
    fun rejectedField_keepsDialogOpen_andReportsErrorInDialog() {
        val fieldsBefore = state.fields
        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)

        viewModel.onEvent(FormUserEvent.OnAddField(newField(key = "full_name")))

        assertEquals(fieldsBefore, state.fields)
        assertTrue(state.isAddFieldDialogVisible)
        assertEquals("A field with key 'full_name' already exists.", state.addFieldError)
        // Definition errors must not leak into the form-level error.
        assertNull(state.formError)
    }

    @Test
    fun dismissingDialog_hidesItAndClearsError() {
        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)
        viewModel.onEvent(FormUserEvent.OnAddField(newField(key = "full_name")))

        viewModel.onEvent(FormUserEvent.OnAddFieldDialogDismissed)

        assertFalse(state.isAddFieldDialogVisible)
        assertNull(state.addFieldError)
    }

    @Test
    fun reopeningDialog_clearsPreviousError() {
        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)
        viewModel.onEvent(FormUserEvent.OnAddField(newField(key = "full_name")))
        viewModel.onEvent(FormUserEvent.OnAddFieldDialogDismissed)

        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)

        assertNull(state.addFieldError)
    }

    @Test
    fun blankLabel_isReportedInDialog() {
        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)

        viewModel.onEvent(FormUserEvent.OnAddField(newField(label = "  ")))

        assertTrue(state.isAddFieldDialogVisible)
        assertEquals("Field label cannot be blank.", state.addFieldError)
    }

    @Test
    fun blankKey_isReportedInDialog() {
        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)

        viewModel.onEvent(FormUserEvent.OnAddField(newField(key = "")))

        assertEquals("Field key cannot be blank.", state.addFieldError)
    }

    @Test
    fun blankOption_isReportedInDialog() {
        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)

        viewModel.onEvent(
            FormUserEvent.OnAddField(
                newField().copy(type = FormFieldType.DROPDOWN, options = listOf("A", " ", "B"))
            )
        )

        assertEquals("Options cannot be blank.", state.addFieldError)
    }

    @Test
    fun addedOptions_areTrimmed() {
        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)

        viewModel.onEvent(
            FormUserEvent.OnAddField(
                newField().copy(type = FormFieldType.DROPDOWN, options = listOf(" A", "B "))
            )
        )

        assertEquals(listOf("A", "B"), state.fields.last().options)
    }

    @Test
    fun editingInput_clearsDialogError() {
        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)
        viewModel.onEvent(FormUserEvent.OnAddField(newField(key = "full_name")))

        viewModel.onEvent(FormUserEvent.OnAddFieldInputChanged)

        assertTrue(state.isAddFieldDialogVisible)
        assertNull(state.addFieldError)
    }

    @Test
    fun openingDialog_clearsStaleFormError() {
        viewModel.onEvent(FormUserEvent.OnSubmitForm)
        assertNotNull(state.formError)

        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)

        assertNull(state.formError)
    }

    @Test
    fun addAfterDialogClosed_isIgnored() {
        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)
        val first = newField(id = "field_a")
        viewModel.onEvent(FormUserEvent.OnAddField(first))
        val before = state

        // A double tap sends the same definition again with a fresh id.
        viewModel.onEvent(FormUserEvent.OnAddField(first.copy(id = "field_b")))

        assertEquals(before, state)
        assertNull(state.addFieldError)
    }

    @Test
    fun dialogVisibility_isRestoredFromSavedState() {
        val handle = SavedStateHandle()
        DynamicFormViewModel(handle).onEvent(FormUserEvent.OnAddFieldDialogOpened)

        val restored = DynamicFormViewModel(handle)

        assertTrue(restored.uiState.value.isAddFieldDialogVisible)
    }

    @Test
    fun closedDialog_isNotRestoredFromSavedState() {
        val handle = SavedStateHandle()
        DynamicFormViewModel(handle).apply {
            onEvent(FormUserEvent.OnAddFieldDialogOpened)
            onEvent(FormUserEvent.OnAddFieldDialogDismissed)
        }

        assertFalse(DynamicFormViewModel(handle).uiState.value.isAddFieldDialogVisible)
    }

    @Test
    fun reset_closesDialogAndClearsError() {
        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)
        viewModel.onEvent(FormUserEvent.OnAddField(newField(key = "full_name")))

        viewModel.onEvent(FormUserEvent.OnResetForm)

        assertFalse(state.isAddFieldDialogVisible)
        assertNull(state.addFieldError)
    }

    // endregion

    // region Submission result lifecycle

    @Test
    fun successfulSubmit_producesResultKeyedByFieldKey() {
        submitSuccessfully()

        assertEquals("Jane Doe", state.submissionResult!!["full_name"])
        assertEquals(true, state.submissionResult!!["terms"])
    }

    @Test
    fun failedSubmit_clearsPreviousResult() {
        submitSuccessfully()
        val nameId = state.fields.first { it.key == "full_name" }.id
        viewModel.onEvent(FormUserEvent.OnFieldValueChanged(nameId, ""))

        viewModel.onEvent(FormUserEvent.OnSubmitForm)

        assertFalse(state.isSuccess)
        assertNull(state.submissionResult)
        assertNotNull(state.formError)
    }

    @Test
    fun valueChange_clearsResult() {
        submitSuccessfully()

        viewModel.onEvent(FormUserEvent.OnFieldValueChanged(state.fields.first().id, "Changed"))

        assertFalse(state.isSuccess)
        assertNull(state.submissionResult)
    }

    @Test
    fun checkboxToggle_clearsResult() {
        submitSuccessfully()
        val termsId = state.fields.first { it.type == FormFieldType.CHECKBOX }.id

        viewModel.onEvent(FormUserEvent.OnCheckboxToggled(termsId, false))

        assertFalse(state.isSuccess)
        assertNull(state.submissionResult)
    }

    @Test
    fun addingField_clearsResult() {
        submitSuccessfully()

        viewModel.onEvent(FormUserEvent.OnAddFieldDialogOpened)
        viewModel.onEvent(FormUserEvent.OnAddField(newField()))

        assertFalse(state.isSuccess)
        assertNull(state.submissionResult)
    }

    @Test
    fun removingField_clearsResult() {
        submitSuccessfully()

        viewModel.onEvent(FormUserEvent.OnRemoveField(state.fields.first().id))

        assertFalse(state.isSuccess)
        assertNull(state.submissionResult)
    }

    @Test
    fun removingUnknownField_leavesStateUnchanged() {
        submitSuccessfully()
        val before = state

        viewModel.onEvent(FormUserEvent.OnRemoveField("does_not_exist"))

        assertEquals(before, state)
    }

    @Test
    fun dismissingResult_clearsIt() {
        submitSuccessfully()

        viewModel.onEvent(FormUserEvent.OnSubmissionResultDismissed)

        assertFalse(state.isSuccess)
        assertNull(state.submissionResult)
    }

    // endregion
}
