package com.example.dynamicform

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dynamicapp.ui.theme.DynamicAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddFieldDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var viewModel: DynamicFormViewModel

    private val state get() = viewModel.uiState.value

    @Before
    fun setUp() {
        viewModel = DynamicFormViewModel()
        composeRule.setContent {
            DynamicAppTheme {
                DynamicFormScreen(viewModel = viewModel)
            }
        }
        composeRule.onNodeWithContentDescription("Add Field").performClick()
    }

    private fun typeLabel(text: String) {
        composeRule.onNodeWithText("Field Label").performTextInput(text)
    }

    private fun selectType(type: FormFieldType) {
        composeRule.onNodeWithText("Type: TEXT").performClick()
        composeRule.onNodeWithText(type.name).performClick()
    }

    private fun clickAdd() {
        composeRule.onNodeWithText("Add").performClick()
    }

    private fun assertDialogShown() {
        composeRule.onNodeWithText("Add Dynamic Field").assertIsDisplayed()
    }

    @Test
    fun emptyLabel_showsErrorInDialog() {
        clickAdd()

        assertDialogShown()
        composeRule.onNodeWithText("Field label cannot be blank.").assertIsDisplayed()
    }

    @Test
    fun clearedKey_showsErrorInDialog() {
        typeLabel("Nickname")
        composeRule.onNodeWithText("Field Key (Data Map Key)").performTextReplacement("")

        clickAdd()

        composeRule.onNodeWithText("Field key cannot be blank.").assertIsDisplayed()
    }

    @Test
    fun editingInput_clearsError() {
        // "Full Name" generates the existing key "full_name".
        typeLabel("Full Name")
        clickAdd()
        composeRule.onNodeWithText("A field with key 'full_name' already exists.").assertIsDisplayed()

        composeRule.onNodeWithText("Field Key (Data Map Key)").performTextReplacement("preferred_name")

        composeRule.onNodeWithText("A field with key 'full_name' already exists.").assertDoesNotExist()
    }

    @Test
    fun validField_isAddedAndDialogCloses() {
        typeLabel("  Nickname  ")

        clickAdd()

        composeRule.onNodeWithText("Add Dynamic Field").assertDoesNotExist()
        assertFalse(state.isAddFieldDialogVisible)
        val added = state.fields.last()
        assertEquals("nickname", added.key)
        assertEquals("Nickname", added.label)
    }

    @Test
    fun placeholderForHiddenInput_isNotSaved() {
        typeLabel("Color")
        composeRule.onNodeWithText("Placeholder").performTextInput("Pick one")
        selectType(FormFieldType.DROPDOWN)
        composeRule.onNodeWithText("Options (comma-separated)").performTextInput(" Red, Blue ")

        clickAdd()

        val added = state.fields.last()
        assertEquals(FormFieldType.DROPDOWN, added.type)
        assertEquals("", added.placeholder)
        assertEquals(listOf("Red", "Blue"), added.options)
    }

    @Test
    fun blankOption_showsErrorInDialog() {
        typeLabel("Color")
        selectType(FormFieldType.DROPDOWN)
        composeRule.onNodeWithText("Options (comma-separated)").performTextInput("Red, Blue,")

        clickAdd()

        assertDialogShown()
        composeRule.onNodeWithText("Options cannot be blank.").assertIsDisplayed()
    }

    @Test
    fun cancel_closesDialogWithoutAddingField() {
        val fieldCount = state.fields.size
        typeLabel("Nickname")

        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Add Dynamic Field").assertDoesNotExist()
        assertEquals(fieldCount, state.fields.size)
    }
}
