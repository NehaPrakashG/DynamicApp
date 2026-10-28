## 🤖 AI Prompt Suite

This library's core architecture was generated and optimized using targeted AI prompts designed around Android's modern Jetpack Compose standards, MVVM pattern, and Unidirectional Data Flow (UDF).

### 1. Dynamic Form Architecture Prompt (State & ViewModel)

> **Role & Task:** Act as a Senior Android Engineer. Design the core state management layer for a Dynamic Form Engine in Kotlin.
>
> **Requirements:**
> - Define a type-safe `FormFieldModel` sealed class/data structure supporting `TextInput`, `SelectableOption`, `Checkbox`, `DatePicker`, and `Slider`.
> - Create an immutable `DynamicFormUiState` containing the form schema, field values (`Map<String, Any>`), validation errors, and overall form validity.
> - Implement a `DynamicFormViewModel` leveraging `StateFlow` and `asStateFlow()` to expose read-only state.
> - Expose explicit user actions (`onFieldValueChanged`, `validateForm`, `submitForm`) to maintain strict Unidirectional Data Flow (UDF). No side effects allowed inside UI elements.

### 2. Jetpack Compose Renderer Prompt (UI & Field Composables)

> **Role & Task:** Act as a Senior Android Compose Specialist. Create the dynamic UI rendering layer for the dynamic form engine.
>
> **Requirements:**
> - Implement a main `DynamicFormScreen` composable consuming `DynamicFormUiState` and emitting UI events up to the `DynamicFormViewModel`.
> - Create a modular `DynamicFieldRenderer` composable that acts as a factory, matching each `FormFieldModel` type to its corresponding Jetpack Compose Material 3 component (`OutlinedTextField`, `Checkbox`, `ExposedDropdownMenuBox`, etc.).
> - Handle real-time inline validation error messages, focus movement, keyboard actions (Next/Done), and enable state preservation across orientation changes (`rememberSaveable`).