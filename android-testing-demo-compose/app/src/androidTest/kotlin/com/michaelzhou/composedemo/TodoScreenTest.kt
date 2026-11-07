package com.michaelzhou.composedemo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michaelzhou.composedemo.todo.TodoScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // TC-004
    @Test
    fun addTask_appearsInList() {
        composeRule.setContent { TodoScreen() }
        composeRule.onNodeWithTag("taskInput").performTextInput("Buy milk")
        composeRule.onNodeWithTag("addBtn").performClick()
        composeRule.onNodeWithText("Buy milk").assertIsDisplayed()
    }

    // TC-005
    @Test
    fun checkTask_marksAsComplete() {
        composeRule.setContent { TodoScreen() }
        composeRule.onNodeWithTag("taskInput").performTextInput("Buy milk")
        composeRule.onNodeWithTag("addBtn").performClick()
        composeRule.onNodeWithTag("cbDone_Buy milk").performClick()
        composeRule.onNodeWithTag("cbDone_Buy milk").assertIsOn()
    }

    // TC-006
    @Test
    fun deleteTask_removesFromList() {
        composeRule.setContent { TodoScreen() }
        composeRule.onNodeWithTag("taskInput").performTextInput("Buy milk")
        composeRule.onNodeWithTag("addBtn").performClick()
        composeRule.onNodeWithTag("btnDelete_Buy milk").performClick()
        composeRule.onNodeWithText("Buy milk").assertDoesNotExist()
    }
}
