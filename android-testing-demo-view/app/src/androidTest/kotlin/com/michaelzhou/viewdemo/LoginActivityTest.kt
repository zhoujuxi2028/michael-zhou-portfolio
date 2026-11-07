package com.michaelzhou.viewdemo

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michaelzhou.viewdemo.login.LoginActivity
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    // TC-001
    @Test
    fun loginButton_disabledWhenFieldsEmpty() {
        onView(withId(R.id.btnLogin)).check(matches(not(isEnabled())))
    }

    // TC-002
    @Test
    fun loginButton_enabledAfterFillingBothFields() {
        onView(withId(R.id.etEmail)).perform(typeText("test@test.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("123456"), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).check(matches(isEnabled()))
    }

    // TC-003
    @Test
    fun loginSuccess_navigatesToTodoScreen() {
        onView(withId(R.id.etEmail)).perform(typeText("test@test.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("123456"), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())
        onView(withId(R.id.rvTodos)).check(matches(isDisplayed()))
    }
}
