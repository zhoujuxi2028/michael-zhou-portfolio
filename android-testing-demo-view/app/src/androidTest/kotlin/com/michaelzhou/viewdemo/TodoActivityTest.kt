package com.michaelzhou.viewdemo

import android.content.Context
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import com.michaelzhou.viewdemo.db.AppDatabase
import com.michaelzhou.viewdemo.db.UserEntity
import com.michaelzhou.viewdemo.todo.TodoActivity
import com.michaelzhou.viewdemo.utils.sha256
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoActivityTest {

    private val testEmail = "test@test.com"

    private fun launchTodoActivity(): ActivityScenario<TodoActivity> {
        val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, TodoActivity::class.java).apply {
            putExtra("user_email", testEmail)
        }
        return ActivityScenario.launch(intent)
    }

    @Before
    fun cleanDb() {
        val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.getInstance(ctx)
        runBlocking {
            db.todoDao().deleteAllForUser(testEmail)
            db.userDao().findByEmail(testEmail) ?: db.userDao().insert(
                UserEntity(email = testEmail, passwordHash = sha256("123456"))
            )
        }
    }

    // TC-004
    @Test
    fun addTask_appearsInList() {
        launchTodoActivity().use {
            onView(withId(R.id.etTask)).perform(typeText("Buy milk"), closeSoftKeyboard())
            onView(withId(R.id.btnAdd)).perform(click())
            onView(withText("Buy milk")).check(matches(isDisplayed()))
        }
    }

    // TC-005
    @Test
    fun checkTask_marksAsComplete() {
        launchTodoActivity().use {
            onView(withId(R.id.etTask)).perform(typeText("Buy milk"), closeSoftKeyboard())
            onView(withId(R.id.btnAdd)).perform(click())
            onView(withId(R.id.cbDone)).perform(click())
            onView(withId(R.id.cbDone)).check(matches(isChecked()))
        }
    }

    // TC-006
    @Test
    fun deleteTask_removesFromList() {
        launchTodoActivity().use {
            onView(withId(R.id.etTask)).perform(typeText("Buy milk"), closeSoftKeyboard())
            onView(withId(R.id.btnAdd)).perform(click())
            onView(withId(R.id.btnDelete)).perform(click())
            onView(withText("Buy milk")).check(doesNotExist())
        }
    }

    // TC-009
    @Test
    fun addTask_persistsAfterActivityRecreation() {
        launchTodoActivity().use { scenario ->
            onView(withId(R.id.etTask)).perform(typeText("Persist me"), closeSoftKeyboard())
            onView(withId(R.id.btnAdd)).perform(click())
            onView(withText("Persist me")).check(matches(isDisplayed()))
            scenario.recreate()
            onView(withText("Persist me")).check(matches(isDisplayed()))
        }
    }
}
