# Android Testing Demo (View/Espresso) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Login + Todo two-screen Android app using XML View layout and validate it with 6 Espresso instrumented tests.

**Architecture:** MVVM with LiveData; LoginActivity → TodoActivity via Intent; RecyclerView for task list. All state lives in ViewModels; Activities only observe and dispatch.

**Tech Stack:** Kotlin 1.9.25, AppCompat, Material Components, RecyclerView, Espresso 3.6.1, AndroidX Test, Java 17, Gradle 8.9 / AGP 8.7.3

**Spec:** `docs/superpowers/specs/2026-11-06-android-testing-demo-design.md`
**Issue:** https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/27

---

## File Map

| File | Responsibility |
|------|---------------|
| `settings.gradle.kts` | Project name, repo config |
| `build.gradle.kts` (root) | AGP + Kotlin plugin versions |
| `app/build.gradle.kts` | Dependencies, testInstrumentationRunner |
| `gradle.properties` | Java 17 path, AndroidX flag |
| `app/src/main/AndroidManifest.xml` | Activity declarations |
| `login/LoginActivity.kt` | UI wiring, observe ViewModel |
| `login/LoginViewModel.kt` | Email/password state → isLoginEnabled LiveData |
| `todo/TodoActivity.kt` | RecyclerView setup, observe ViewModel |
| `todo/TodoViewModel.kt` | List state: add / toggle / delete |
| `todo/TodoAdapter.kt` | RecyclerView adapter for TodoItem |
| `todo/TodoItem.kt` | Data class |
| `res/layout/activity_login.xml` | Email + Password fields, Login button, error text |
| `res/layout/activity_todo.xml` | Input row + RecyclerView |
| `res/layout/item_todo.xml` | Checkbox + Task text + Delete button |
| `LoginActivityTest.kt` | TC-001, TC-002, TC-003 |
| `TodoActivityTest.kt` | TC-004, TC-005, TC-006 |

---

## Task 1: Project Scaffold

**Files:**
- Create: `android-testing-demo-view/settings.gradle.kts`
- Create: `android-testing-demo-view/build.gradle.kts`
- Create: `android-testing-demo-view/gradle.properties`
- Create: `android-testing-demo-view/app/build.gradle.kts`
- Create: `android-testing-demo-view/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "android-testing-demo-view"
include(":app")
```

- [ ] **Step 2: Create root `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
}
```

- [ ] **Step 3: Create `gradle.properties`**

```properties
android.useAndroidX=true
org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

- [ ] **Step 4: Create `app/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.michaelzhou.viewdemo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.michaelzhou.viewdemo"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
}
```

- [ ] **Step 5: Create `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="ViewDemo"
        android:theme="@style/Theme.MaterialComponents.DayNight.NoActionBar">
        <activity android:name=".login.LoginActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
        <activity android:name=".todo.TodoActivity" android:exported="false"/>
    </application>
</manifest>
```

- [ ] **Step 6: Copy Gradle wrapper from android-app**

```bash
cp -r android-app/gradle android-testing-demo-view/
cp android-app/gradlew android-testing-demo-view/
cp android-app/gradlew.bat android-testing-demo-view/ 2>/dev/null || true
chmod +x android-testing-demo-view/gradlew
```

- [ ] **Step 7: Verify build**

```bash
cd android-testing-demo-view && ./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add android-testing-demo-view/
git commit -m "feat(view): scaffold android-testing-demo-view project (#27)"
```

---

## Task 2: Data Model + TodoItem

**Files:**
- Create: `app/src/main/kotlin/com/michaelzhou/viewdemo/todo/TodoItem.kt`

- [ ] **Step 1: Create `TodoItem.kt`**

```kotlin
package com.michaelzhou.viewdemo.todo

data class TodoItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isDone: Boolean = false
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/com/michaelzhou/viewdemo/todo/TodoItem.kt
git commit -m "feat(view): add TodoItem data class (#27)"
```

---

## Task 3: Login — Layouts + ViewModel + Activity

**Files:**
- Create: `app/src/main/res/layout/activity_login.xml`
- Create: `app/src/main/kotlin/com/michaelzhou/viewdemo/login/LoginViewModel.kt`
- Create: `app/src/main/kotlin/com/michaelzhou/viewdemo/login/LoginActivity.kt`

- [ ] **Step 1: Create `activity_login.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="24dp"
    android:gravity="center">

    <EditText
        android:id="@+id/etEmail"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Email"
        android:inputType="textEmailAddress" />

    <EditText
        android:id="@+id/etPassword"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Password"
        android:inputType="textPassword"
        android:layout_marginTop="16dp" />

    <TextView
        android:id="@+id/tvError"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="请填写所有字段"
        android:textColor="@android:color/holo_red_dark"
        android:visibility="gone"
        android:layout_marginTop="8dp" />

    <Button
        android:id="@+id/btnLogin"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Login"
        android:enabled="false"
        android:layout_marginTop="24dp" />

</LinearLayout>
```

- [ ] **Step 2: Create `LoginViewModel.kt`**

```kotlin
package com.michaelzhou.viewdemo.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {
    private var email = ""
    private var password = ""

    private val _isLoginEnabled = MutableLiveData(false)
    val isLoginEnabled: LiveData<Boolean> = _isLoginEnabled

    fun onEmailChanged(value: String) {
        email = value
        _isLoginEnabled.value = email.isNotBlank() && password.isNotBlank()
    }

    fun onPasswordChanged(value: String) {
        password = value
        _isLoginEnabled.value = email.isNotBlank() && password.isNotBlank()
    }
}
```

- [ ] **Step 3: Create `LoginActivity.kt`**

```kotlin
package com.michaelzhou.viewdemo.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.michaelzhou.viewdemo.R
import com.michaelzhou.viewdemo.todo.TodoActivity

class LoginActivity : AppCompatActivity() {
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvError = findViewById<TextView>(R.id.tvError)

        viewModel.isLoginEnabled.observe(this) { btnLogin.isEnabled = it }

        etEmail.addTextChangedListener(onChanged { viewModel.onEmailChanged(it) })
        etPassword.addTextChangedListener(onChanged { viewModel.onPasswordChanged(it) })

        btnLogin.setOnClickListener {
            tvError.visibility = View.GONE
            startActivity(Intent(this, TodoActivity::class.java))
        }
    }

    private fun onChanged(action: (String) -> Unit) = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { action(s.toString()) }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
}
```

- [ ] **Step 4: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/
git commit -m "feat(view): add Login screen - layout, ViewModel, Activity (#27)"
```

---

## Task 4: Login Espresso Tests (TC-001, TC-002, TC-003)

**Files:**
- Create: `app/src/androidTest/kotlin/com/michaelzhou/viewdemo/LoginActivityTest.kt`

- [ ] **Step 1: Create `LoginActivityTest.kt`**

```kotlin
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
```

- [ ] **Step 2: Run tests on emulator/device**

```bash
./gradlew connectedAndroidTest --tests "com.michaelzhou.viewdemo.LoginActivityTest"
```
Expected: TC-001 PASS, TC-002 PASS, TC-003 FAIL (rvTodos not yet exists)

- [ ] **Step 3: Commit tests**

```bash
git add app/src/androidTest/
git commit -m "test(view): add Login Espresso tests TC-001~003 (#27)"
```

---

## Task 5: Todo — Layouts + ViewModel + Adapter + Activity

**Files:**
- Create: `app/src/main/res/layout/activity_todo.xml`
- Create: `app/src/main/res/layout/item_todo.xml`
- Create: `app/src/main/kotlin/com/michaelzhou/viewdemo/todo/TodoViewModel.kt`
- Create: `app/src/main/kotlin/com/michaelzhou/viewdemo/todo/TodoAdapter.kt`
- Create: `app/src/main/kotlin/com/michaelzhou/viewdemo/todo/TodoActivity.kt`

- [ ] **Step 1: Create `activity_todo.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <EditText
            android:id="@+id/etTask"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:hint="New task" />

        <Button
            android:id="@+id/btnAdd"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Add"
            android:layout_marginStart="8dp" />
    </LinearLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvTodos"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_marginTop="16dp" />

</LinearLayout>
```

- [ ] **Step 2: Create `item_todo.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="8dp"
    android:gravity="center_vertical">

    <CheckBox
        android:id="@+id/cbDone"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />

    <TextView
        android:id="@+id/tvTask"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:layout_marginStart="8dp"
        android:textSize="16sp" />

    <Button
        android:id="@+id/btnDelete"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="×"
        android:textSize="18sp" />

</LinearLayout>
```

- [ ] **Step 3: Create `TodoViewModel.kt`**

```kotlin
package com.michaelzhou.viewdemo.todo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TodoViewModel : ViewModel() {
    private val _todos = MutableLiveData<List<TodoItem>>(emptyList())
    val todos: LiveData<List<TodoItem>> = _todos

    fun addTodo(text: String) {
        if (text.isBlank()) return
        _todos.value = _todos.value.orEmpty() + TodoItem(text = text)
    }

    fun toggleDone(id: Long) {
        _todos.value = _todos.value.orEmpty().map {
            if (it.id == id) it.copy(isDone = !it.isDone) else it
        }
    }

    fun deleteTodo(id: Long) {
        _todos.value = _todos.value.orEmpty().filter { it.id != id }
    }
}
```

- [ ] **Step 4: Create `TodoAdapter.kt`**

```kotlin
package com.michaelzhou.viewdemo.todo

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.michaelzhou.viewdemo.R

class TodoAdapter(
    private val onToggle: (Long) -> Unit,
    private val onDelete: (Long) -> Unit
) : RecyclerView.Adapter<TodoAdapter.ViewHolder>() {

    private var items: List<TodoItem> = emptyList()

    fun submitList(list: List<TodoItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cbDone: CheckBox = view.findViewById(R.id.cbDone)
        private val tvTask: TextView = view.findViewById(R.id.tvTask)
        private val btnDelete: Button = view.findViewById(R.id.btnDelete)

        fun bind(item: TodoItem) {
            tvTask.text = item.text
            cbDone.isChecked = item.isDone
            tvTask.paintFlags = if (item.isDone)
                tvTask.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else
                tvTask.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            cbDone.setOnClickListener { onToggle(item.id) }
            btnDelete.setOnClickListener { onDelete(item.id) }
        }
    }
}
```

- [ ] **Step 5: Create `TodoActivity.kt`**

```kotlin
package com.michaelzhou.viewdemo.todo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.michaelzhou.viewdemo.R

class TodoActivity : AppCompatActivity() {
    private val viewModel: TodoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo)

        val etTask = findViewById<EditText>(R.id.etTask)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val rvTodos = findViewById<RecyclerView>(R.id.rvTodos)

        val adapter = TodoAdapter(
            onToggle = { viewModel.toggleDone(it) },
            onDelete = { viewModel.deleteTodo(it) }
        )
        rvTodos.layoutManager = LinearLayoutManager(this)
        rvTodos.adapter = adapter

        viewModel.todos.observe(this) { adapter.submitList(it) }

        btnAdd.setOnClickListener {
            viewModel.addTodo(etTask.text.toString())
            etTask.text.clear()
        }
    }
}
```

- [ ] **Step 6: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/
git commit -m "feat(view): add Todo screen - layout, ViewModel, Adapter, Activity (#27)"
```

---

## Task 6: Todo Espresso Tests (TC-004, TC-005, TC-006)

**Files:**
- Create: `app/src/androidTest/kotlin/com/michaelzhou/viewdemo/TodoActivityTest.kt`

- [ ] **Step 1: Create `TodoActivityTest.kt`**

```kotlin
package com.michaelzhou.viewdemo

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.michaelzhou.viewdemo.todo.TodoActivity
import org.hamcrest.Matchers.not
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
```

- [ ] **Step 2: Run all 6 tests**

```bash
./gradlew connectedAndroidTest
```
Expected: 6 tests PASS, 0 failures

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/
git commit -m "test(view): add Todo Espresso tests TC-004~006 (#27)"
```

---

## Task 7: README

**Files:**
- Create: `android-testing-demo-view/README.md`

- [ ] **Step 1: Create `README.md`**

```markdown
# Android Testing Demo — View (Espresso)

Login + Todo two-screen app demonstrating Espresso UI automation on XML View layout.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| UI | XML Layout + Material Components |
| Architecture | MVVM + LiveData |
| Testing | Espresso 3.6.1 |

## Run Tests

Requires a connected Android device or running emulator (minSdk 29).

```bash
./gradlew connectedAndroidTest
```

## Test Cases

| ID | Scenario |
|----|---------|
| TC-001 | Login button disabled when fields empty |
| TC-002 | Login button enabled after filling both fields |
| TC-003 | Login navigates to Todo screen |
| TC-004 | Adding task appears in list |
| TC-005 | Checking task marks as complete |
| TC-006 | Deleting task removes from list |
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs(view): add README with test instructions (#27)"
```
