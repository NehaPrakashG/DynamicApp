package com.example.dynamicform

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DynamicFormViewModel : ViewModel() {

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

    private val _uiState = MutableStateFlow(DynamicFormUiState(fields = initialFields))
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
                    state.copy(fields = updatedFields, formError = null, isSuccess = false)
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
                    state.copy(fields = updatedFields, formError = null, isSuccess = false)
                }
            }
            is FormUserEvent.OnAddField -> {
                _uiState.update { state ->
                    state.copy(fields = state.fields + event.fieldModel, formError = null)
                }
            }
            is FormUserEvent.OnRemoveField -> {
                _uiState.update { state ->
                    state.copy(fields = state.fields.filter { it.id != event.id }, formError = null)
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
                _uiState.update {
                    DynamicFormUiState(
                        fields = initialFields.map { it.copy(currentValue = "", isChecked = false, errorMessage = null) },
                        isSubmitting = false,
                        isSuccess = false,
                        submissionResult = null,
                        formError = null
                    )
                }
            }
        }
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
                    isSuccess = false
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
}
