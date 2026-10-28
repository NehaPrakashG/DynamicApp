package com.example.dynamicform

enum class FormFieldType {
    TEXT,
    NUMBER,
    EMAIL,
    TEXTAREA,
    DROPDOWN,
    CHECKBOX,
    RADIO,
    DATE_PICKER
}

sealed interface ValidationRule {
    data object Required : ValidationRule
    data class MinLength(val length: Int) : ValidationRule
    data class Regex(val pattern: String, val message: String = "Invalid format") : ValidationRule
}

data class FormFieldModel(
    val id: String,
    val key: String,
    val label: String,
    val type: FormFieldType,
    val placeholder: String = "",
    val options: List<String> = emptyList(),
    val validationRules: List<ValidationRule> = emptyList(),
    val currentValue: String = "",
    val isChecked: Boolean = false,
    val errorMessage: String? = null
)

data class DynamicFormUiState(
    val fields: List<FormFieldModel> = emptyList(),
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val submissionResult: Map<String, Any>? = null,
    val formError: String? = null
)

sealed interface FormUserEvent {
    data class OnFieldValueChanged(val id: String, val value: String) : FormUserEvent
    data class OnCheckboxToggled(val id: String, val isChecked: Boolean) : FormUserEvent
    data class OnAddField(val fieldModel: FormFieldModel) : FormUserEvent
    data class OnRemoveField(val id: String) : FormUserEvent
    data class OnReorderFields(val fromIndex: Int, val toIndex: Int) : FormUserEvent
    data object OnSubmitForm : FormUserEvent
    data object OnResetForm : FormUserEvent

    //The user has dismissed the pending submission result.
    data object OnSubmissionResultDismissed : FormUserEvent
}
