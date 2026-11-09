package com.michaelzhou.viewdemo

import android.content.Context
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.michaelzhou.viewdemo.db.AppDatabase
import com.michaelzhou.viewdemo.db.UserEntity
import com.michaelzhou.viewdemo.login.LoginActivity
import com.michaelzhou.viewdemo.utils.sha256
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

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

    // TC-007
    @Test
    fun wrongPassword_showsErrorMessage() {
        onView(withId(R.id.etEmail)).perform(typeText("test@test.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("wrongpass"), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())
        onView(withId(R.id.tvLoginError)).check(matches(isDisplayed()))
        onView(withId(R.id.tvLoginError)).check(matches(withText("Invalid email or password")))
    }

    // TC-008
    @Test
    fun unregisteredEmail_showsErrorMessage() {
        onView(withId(R.id.etEmail)).perform(typeText("nobody@test.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("anypassword"), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())
        onView(withId(R.id.tvLoginError)).check(matches(isDisplayed()))
        onView(withId(R.id.tvLoginError)).check(matches(withText("Invalid email or password")))
    }
}
