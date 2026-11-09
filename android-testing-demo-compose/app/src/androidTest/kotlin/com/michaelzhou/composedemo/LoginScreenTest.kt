package com.michaelzhou.composedemo

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.michaelzhou.composedemo.db.AppDatabase
import com.michaelzhou.composedemo.db.UserEntity
import com.michaelzhou.composedemo.utils.sha256
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun seedDb() {
        val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.getInstance(ctx)
        runBlocking {
            db.userDao().findByEmail("test@test.com") ?: db.userDao().insert(
                UserEntity(email = "test@test.com", passwordHash = sha256("123456"))
            )
        }
    }

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

    // TC-007
    @Test
    fun wrongPassword_showsErrorMessage() {
        composeRule.onNodeWithTag("emailField").performTextInput("test@test.com")
        composeRule.onNodeWithTag("passwordField").performTextInput("wrongpass")
        composeRule.onNodeWithTag("loginBtn").performClick()
        composeRule.onNodeWithTag("loginError").assertIsDisplayed()
    }

    // TC-008
    @Test
    fun unregisteredEmail_showsErrorMessage() {
        composeRule.onNodeWithTag("emailField").performTextInput("nobody@test.com")
        composeRule.onNodeWithTag("passwordField").performTextInput("anypassword")
        composeRule.onNodeWithTag("loginBtn").performClick()
        composeRule.onNodeWithTag("loginError").assertIsDisplayed()
    }
}
