package com.michaelzhou.composedemo

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
class TodoScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setupDb() {
        val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.getInstance(ctx)
        runBlocking {
            db.todoDao().deleteAllForUser("test@test.com")
            db.userDao().findByEmail("test@test.com") ?: db.userDao().insert(
                UserEntity(email = "test@test.com", passwordHash = sha256("123456"))
            )
        }
    }

    private fun login() {
        composeRule.onNodeWithTag("emailField").performTextInput("test@test.com")
        composeRule.onNodeWithTag("passwordField").performTextInput("123456")
        composeRule.onNodeWithTag("loginBtn").performClick()
        composeRule.onNodeWithTag("todoList").assertIsDisplayed()
    }

    // TC-004
    @Test
    fun addTask_appearsInList() {
        login()
        composeRule.onNodeWithTag("taskInput").performTextInput("Buy milk")
        composeRule.onNodeWithTag("addBtn").performClick()
        composeRule.onNodeWithText("Buy milk").assertIsDisplayed()
    }

    // TC-005
    @Test
    fun checkTask_marksAsComplete() {
        login()
        composeRule.onNodeWithTag("taskInput").performTextInput("Buy milk")
        composeRule.onNodeWithTag("addBtn").performClick()
        composeRule.onNodeWithTag("cbDone_Buy milk").performClick()
        composeRule.onNodeWithTag("cbDone_Buy milk").assertIsOn()
    }

    // TC-006
    @Test
    fun deleteTask_removesFromList() {
        login()
        composeRule.onNodeWithTag("taskInput").performTextInput("Buy milk")
        composeRule.onNodeWithTag("addBtn").performClick()
        composeRule.onNodeWithTag("btnDelete_Buy milk").performClick()
        composeRule.onNodeWithText("Buy milk").assertDoesNotExist()
    }

    // TC-009
    @Test
    fun addTask_persistsAfterActivityRecreation() {
        login()
        composeRule.onNodeWithTag("taskInput").performTextInput("Persist me")
        composeRule.onNodeWithTag("addBtn").performClick()
        composeRule.onNodeWithText("Persist me").assertIsDisplayed()
        composeRule.activityRule.scenario.recreate()
        login()
        composeRule.onNodeWithText("Persist me").assertIsDisplayed()
    }
}
