package com.example.dynamicform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DynamicFormViewModel(
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {
    private val fieldDefinitionValidator = FieldDefinitionValidator()

    private val initialFields = listOf(
        FormFieldModel(
            id = "field_1",
            key = "full_name",
            label = "Full Name",
            type = FormFieldType.TEXT,
            placeholder = "Enter your full name",
            validationRules = listOf(
                ValidationRule.Required,
                ValidationRule.MinLength(2)
            )
        ),
        FormFieldModel(
            id = "field_2",
            key = "email",
            label = "Email Address",
            type = FormFieldType.EMAIL,
            placeholder = "name@example.com",
            validationRules = listOf(
                ValidationRule.Required,
                ValidationRule.Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", "Please enter a valid email address")
            )
        ),
        FormFieldModel(
            id = "field_3",
            key = "age",
            label = "Age",
            type = FormFieldType.NUMBER,
            placeholder = "Enter your age",
            validationRules = listOf(
                ValidationRule.Required
            )
        ),
        FormFieldModel(
            id = "field_4",
            key = "bio",
            label = "Biography",
            type = FormFieldType.TEXTAREA,
            placeholder = "Tell us about yourself...",
            validationRules = listOf(
                ValidationRule.MinLength(10)
            )
        ),
        FormFieldModel(
            id = "field_5",
            key = "gender",
            label = "Gender",
            type = FormFieldType.RADIO,
            options = listOf("Male", "Female", "Other", "Prefer not to say"),
            validationRules = listOf(
                ValidationRule.Required
            )
        ),
        FormFieldModel(
            id = "field_6",
            key = "country",
            label = "Country",
            type = FormFieldType.DROPDOWN,
            options = listOf("United States", "Canada", "United Kingdom", "Australia", "Germany", "India"),
            validationRules = listOf(
                ValidationRule.Required
            )
        ),
        FormFieldModel(
            id = "field_7",
            key = "birthdate",
            label = "Date of Birth",
            type = FormFieldType.DATE_PICKER,
            placeholder = "YYYY-MM-DD",
            validationRules = listOf(
                ValidationRule.Required
            )
        ),
        FormFieldModel(
            id = "field_8",
            key = "terms",
            label = "I agree to the terms and conditions",
            type = FormFieldType.CHECKBOX,
            validationRules = listOf(
                ValidationRule.Required
            )
        )
    )

    private val _uiState = MutableStateFlow(
        DynamicFormUiState(
            fields = initialFields,
            // Restore the open dialog after process death; its inputs use rememberSaveable.
            isAddFieldDialogVisible = savedStateHandle[KEY_ADD_FIELD_DIALOG_VISIBLE] ?: false
        )
    )
    val uiState: StateFlow<DynamicFormUiState> = _uiState.asStateFlow()

    fun onEvent(event: FormUserEvent) {
        when (event) {
            is FormUserEvent.OnFieldValueChanged -> {
                _uiState.update { state ->
                    val updatedFields = state.fields.map { field ->
                        if (field.id == event.id) {
                            val newField = field.copy(currentValue = event.value, errorMessage = null)
                            validateField(newField)
                        } else {
                            field
                        }
                    }
                    state.copy(fields = updatedFields, formError = null, isSuccess = false, submissionResult = null)
                }
            }
            is FormUserEvent.OnCheckboxToggled -> {
                _uiState.update { state ->
                    val updatedFields = state.fields.map { field ->
                        if (field.id == event.id) {
                            val newField = field.copy(isChecked = event.isChecked, errorMessage = null)
                            validateField(newField)
                        } else {
                            field
                        }
                    }
                    state.copy(fields = updatedFields, formError = null, isSuccess = false, submissionResult = null)
                }
            }
            is FormUserEvent.OnAddFieldDialogOpened -> {
                _uiState.update { state ->
                    // The user is now editing the form's structure, so an earlier submit error is stale.
                    state.copy(isAddFieldDialogVisible = true, addFieldError = null, formError = null)
                }
            }
            is FormUserEvent.OnAddFieldInputChanged -> {
                _uiState.update { state ->
                    // The error described the previous input, not the edited one.
                    state.copy(addFieldError = null)
                }
            }
            is FormUserEvent.OnAddFieldDialogDismissed -> {
                _uiState.update { state ->
                    state.copy(isAddFieldDialogVisible = false, addFieldError = null)
                }
            }
            is FormUserEvent.OnAddField -> {
                addField(event.fieldModel)
            }
            is FormUserEvent.OnRemoveField -> {
                _uiState.update { state ->
                    // Ignore removal requests for fields that no longer exist.
                    if (state.fields.none { it.id == event.id }) {
                        state
                    } else {
                        state.copy(
                            fields = state.fields.filterNot {
                                it.id == event.id
                            },
                            formError = null,
                            isSuccess = false,
                            submissionResult = null
                        )
                    }
                }
            }
            is FormUserEvent.OnReorderFields -> {
                _uiState.update { state ->
                    val mutableList = state.fields.toMutableList()
                    if (event.fromIndex in mutableList.indices && event.toIndex in mutableList.indices) {
                        val item = mutableList.removeAt(event.fromIndex)
                        mutableList.add(event.toIndex, item)
                    }
                    state.copy(fields = mutableList)
                }
            }
            is FormUserEvent.OnSubmitForm -> {
                submitForm()
            }
            is FormUserEvent.OnResetForm -> {
                _uiState.update { state ->
                    state.copy(
                        fields = initialFields.map { it.copy(currentValue = "", isChecked = false, errorMessage = null) },
                        isSubmitting = false,
                        isSuccess = false,
                        submissionResult = null,
                        formError = null,
                        // Resetting also discards any field definition in progress.
                        isAddFieldDialogVisible = false,
                        addFieldError = null
                    )
                }
            }
           is  FormUserEvent.OnSubmissionResultDismissed -> {
                _uiState.update { state ->
                    // Clear the result at its source, not only in the screen.
                    state.copy(
                        isSuccess = false,
                        submissionResult = null
                    )
                }
            }
        }

        savedStateHandle[KEY_ADD_FIELD_DIALOG_VISIBLE] = _uiState.value.isAddFieldDialogVisible
    }

    private fun validateField(field: FormFieldModel): FormFieldModel {
        for (rule in field.validationRules) {
            val error = when (rule) {
                is ValidationRule.Required -> {
                    if (field.type == FormFieldType.CHECKBOX) {
                        if (!field.isChecked) "This field is required" else null
                    } else {
                        if (field.currentValue.isBlank()) "This field is required" else null
                    }
                }
                is ValidationRule.MinLength -> {
                    if (field.currentValue.isNotBlank() && field.currentValue.length < rule.length) {
                        "Minimum length is ${rule.length} characters"
                    } else null
                }
                is ValidationRule.Regex -> {
                    if (field.currentValue.isNotBlank() && !Regex(rule.pattern).matches(field.currentValue)) {
                        rule.message
                    } else null
                }
            }
            if (error != null) {
                return field.copy(errorMessage = error)
            }
        }
        return field.copy(errorMessage = null)
    }

    private fun submitForm() {
        _uiState.update { state ->
            var hasError = false
            val validatedFields = state.fields.map { field ->
                val validated = validateField(field)
                if (validated.errorMessage != null) {
                    hasError = true
                }
                validated
            }

            if (hasError) {
                state.copy(
                    fields = validatedFields,
                    formError = "Please correct the validation errors before submitting.",
                    isSubmitting = false,
                    isSuccess = false,

                    // Do not retain an older successful result after a failed submission.
                    submissionResult = null
                )
            } else {
                val resultMap = mutableMapOf<String, Any>()
                validatedFields.forEach { f ->
                    if (f.type == FormFieldType.CHECKBOX) {
                        resultMap[f.key] = f.isChecked
                    } else {
                        resultMap[f.key] = f.currentValue
                    }
                }
                state.copy(
                    fields = validatedFields,
                    isSubmitting = false,
                    isSuccess = true,
                    submissionResult = resultMap,
                    formError = null
                )
            }
        }
    }

    private fun addField(candidate: FormFieldModel) {
        // The single place where dialog input is normalized; the screen passes raw input.
        val normalizedField = candidate.copy(
            key = candidate.key.trim(),
            label = candidate.label.trim(),
            options = candidate.options.map { it.trim() }
        )

        _uiState.update { state ->
            // Ignore a late submission (e.g. a double tap) after the dialog has closed.
            if (!state.isAddFieldDialogVisible) {
                return@update state
            }

            // Check against the same state snapshot used for the update.
            val error = fieldDefinitionValidator.validate(
                candidate = normalizedField,
                existingFields = state.fields
            )

            if (error != null) {
                // Keep the dialog open so the user can correct the definition.
                state.copy(addFieldError = error)
            } else {
                state.copy(
                    fields = state.fields + normalizedField,
                    formError = null,
                    isAddFieldDialogVisible = false,
                    addFieldError = null,

                    // A changed form no longer represents the previous submission.
                    isSuccess = false,
                    submissionResult = null
                )
            }
        }
    }

    private companion object {
        const val KEY_ADD_FIELD_DIALOG_VISIBLE = "add_field_dialog_visible"
    }
}
