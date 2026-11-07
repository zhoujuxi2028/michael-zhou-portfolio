package com.michaelzhou.composedemo

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // TC-001
    @Test
    fun loginButton_disabledWhenFieldsEmpty() {
        composeRule.onNodeWithTag("loginBtn").assertIsNotEnabled()
    }

    // TC-002
    @Test
    fun loginButton_enabledAfterFillingBothFields() {
        composeRule.onNodeWithTag("emailField").performTextInput("test@test.com")
        composeRule.onNodeWithTag("passwordField").performTextInput("123456")
        composeRule.onNodeWithTag("loginBtn").assertIsEnabled()
    }

    // TC-003
    @Test
    fun loginSuccess_navigatesToTodoScreen() {
        composeRule.onNodeWithTag("emailField").performTextInput("test@test.com")
        composeRule.onNodeWithTag("passwordField").performTextInput("123456")
        composeRule.onNodeWithTag("loginBtn").performClick()
        composeRule.onNodeWithTag("todoList").assertIsDisplayed()
    }
}
