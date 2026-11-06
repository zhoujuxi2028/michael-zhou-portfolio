package com.michaelzhou.viewdemo

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michaelzhou.viewdemo.todo.TodoActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(TodoActivity::class.java)

    // TC-004
    @Test
    fun addTask_appearsInList() {
        onView(withId(R.id.etTask)).perform(typeText("Buy milk"), closeSoftKeyboard())
        onView(withId(R.id.btnAdd)).perform(click())
        onView(withText("Buy milk")).check(matches(isDisplayed()))
    }

    // TC-005
    @Test
    fun checkTask_marksAsComplete() {
        onView(withId(R.id.etTask)).perform(typeText("Buy milk"), closeSoftKeyboard())
        onView(withId(R.id.btnAdd)).perform(click())
        onView(withId(R.id.cbDone)).perform(click())
        onView(withId(R.id.cbDone)).check(matches(isChecked()))
    }

    // TC-006
    @Test
    fun deleteTask_removesFromList() {
        onView(withId(R.id.etTask)).perform(typeText("Buy milk"), closeSoftKeyboard())
        onView(withId(R.id.btnAdd)).perform(click())
        onView(withId(R.id.btnDelete)).perform(click())
        onView(withText("Buy milk")).check(doesNotExist())
    }
}
