# Android Testing Demo (Compose/UI Test) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Login + Todo two-screen Android app using Jetpack Compose and validate it with 6 Compose UI Test instrumented tests.

**Architecture:** MVVM with StateFlow; Navigation Compose (NavHost) for screen routing; all UI state in ViewModels exposed as StateFlow. Composables are stateless — they receive state and emit events.

**Tech Stack:** Kotlin 1.9.25, Jetpack Compose BOM 2024.10.01, Material3, Navigation Compose 2.8.4, Compose UI Test (ui-test-junit4), Java 17, Gradle 8.9 / AGP 8.7.3

**Spec:** `docs/superpowers/specs/2026-11-06-android-testing-demo-design.md`
**Issue:** https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/27

---

## File Map

| File | Responsibility |
|------|---------------|
| `settings.gradle.kts` | Project name, repo config |
| `build.gradle.kts` (root) | AGP + Kotlin plugin versions |
| `app/build.gradle.kts` | Compose dependencies, testInstrumentationRunner |
| `gradle.properties` | Java 17 path, AndroidX flag |
| `AndroidManifest.xml` | Single MainActivity declaration |
| `MainActivity.kt` | Entry point, hosts AppNavigation |
| `navigation/AppNavigation.kt` | NavHost: login → todo routes |
| `login/LoginViewModel.kt` | email/password → isLoginEnabled StateFlow |
| `login/LoginScreen.kt` | Compose UI: email, password, button (testTags) |
| `todo/TodoItem.kt` | Data class |
| `todo/TodoViewModel.kt` | List state: add / toggle / delete |
| `todo/TodoScreen.kt` | Compose UI: input, LazyColumn, item rows (testTags) |
| `LoginScreenTest.kt` | TC-001, TC-002, TC-003 |
| `TodoScreenTest.kt` | TC-004, TC-005, TC-006 |

---

## Task 1: Project Scaffold

**Files:**
- Create: `android-testing-demo-compose/settings.gradle.kts`
- Create: `android-testing-demo-compose/build.gradle.kts`
- Create: `android-testing-demo-compose/gradle.properties`
- Create: `android-testing-demo-compose/app/build.gradle.kts`
- Create: `android-testing-demo-compose/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "android-testing-demo-compose"
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
    namespace = "com.michaelzhou.composedemo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.michaelzhou.composedemo"
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

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

- [ ] **Step 5: Create `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="ComposeDemo"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 6: Copy Gradle wrapper from android-app**

```bash
cp -r android-app/gradle android-testing-demo-compose/
cp android-app/gradlew android-testing-demo-compose/
chmod +x android-testing-demo-compose/gradlew
```

- [ ] **Step 7: Verify build**

```bash
cd android-testing-demo-compose && ./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add android-testing-demo-compose/
git commit -m "feat(compose): scaffold android-testing-demo-compose project (#27)"
```

---

## Task 2: Data Model + Navigation + MainActivity

**Files:**
- Create: `app/src/main/kotlin/com/michaelzhou/composedemo/todo/TodoItem.kt`
- Create: `app/src/main/kotlin/com/michaelzhou/composedemo/navigation/AppNavigation.kt`
- Create: `app/src/main/kotlin/com/michaelzhou/composedemo/MainActivity.kt`

- [ ] **Step 1: Create `TodoItem.kt`**

```kotlin
package com.michaelzhou.composedemo.todo

data class TodoItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isDone: Boolean = false
)
```

- [ ] **Step 2: Create `AppNavigation.kt`**

```kotlin
package com.michaelzhou.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.michaelzhou.composedemo.login.LoginScreen
import com.michaelzhou.composedemo.todo.TodoScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(onLoginSuccess = { navController.navigate("todo") })
        }
        composable("todo") {
            TodoScreen()
        }
    }
}
```

- [ ] **Step 3: Create `MainActivity.kt`**

```kotlin
package com.michaelzhou.composedemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.michaelzhou.composedemo.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigation()
            }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/
git commit -m "feat(compose): add TodoItem, AppNavigation, MainActivity (#27)"
```

---

## Task 3: Login — ViewModel + Screen

**Files:**
- Create: `app/src/main/kotlin/com/michaelzhou/composedemo/login/LoginViewModel.kt`
- Create: `app/src/main/kotlin/com/michaelzhou/composedemo/login/LoginScreen.kt`

- [ ] **Step 1: Create `LoginViewModel.kt`**

```kotlin
package com.michaelzhou.composedemo.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class LoginViewModel : ViewModel() {
    private val _email = MutableStateFlow("")
    private val _password = MutableStateFlow("")

    val isLoginEnabled: StateFlow<Boolean> = combine(_email, _password) { email, password ->
        email.isNotBlank() && password.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onEmailChanged(value: String) { _email.value = value }
    fun onPasswordChanged(value: String) { _password.value = value }
}
```

- [ ] **Step 2: Create `LoginScreen.kt`**

```kotlin
package com.michaelzhou.composedemo.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val isEnabled by viewModel.isLoginEnabled.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; viewModel.onEmailChanged(it) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().testTag("emailField")
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.onPasswordChanged(it) },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().testTag("passwordField")
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onLoginSuccess,
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth().testTag("loginBtn")
        ) { Text("Login") }
    }
}
```

- [ ] **Step 3: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/michaelzhou/composedemo/login/
git commit -m "feat(compose): add Login ViewModel and Screen (#27)"
```

---

## Task 4: Login Compose UI Tests (TC-001, TC-002, TC-003)

**Files:**
- Create: `app/src/androidTest/kotlin/com/michaelzhou/composedemo/LoginScreenTest.kt`

- [ ] **Step 1: Create `LoginScreenTest.kt`**

```kotlin
package com.michaelzhou.composedemo

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
```

- [ ] **Step 2: Run tests**

```bash
./gradlew connectedAndroidTest --tests "com.michaelzhou.composedemo.LoginScreenTest"
```
Expected: TC-001 PASS, TC-002 PASS, TC-003 FAIL (todoList tag not yet exists)

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/
git commit -m "test(compose): add Login Compose UI tests TC-001~003 (#27)"
```

---

## Task 5: Todo — ViewModel + Screen

**Files:**
- Create: `app/src/main/kotlin/com/michaelzhou/composedemo/todo/TodoViewModel.kt`
- Create: `app/src/main/kotlin/com/michaelzhou/composedemo/todo/TodoScreen.kt`

- [ ] **Step 1: Create `TodoViewModel.kt`**

```kotlin
package com.michaelzhou.composedemo.todo

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TodoViewModel : ViewModel() {
    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    fun addTodo(text: String) {
        if (text.isBlank()) return
        _todos.value = _todos.value + TodoItem(text = text)
    }

    fun toggleDone(id: Long) {
        _todos.value = _todos.value.map {
            if (it.id == id) it.copy(isDone = !it.isDone) else it
        }
    }

    fun deleteTodo(id: Long) {
        _todos.value = _todos.value.filter { it.id != id }
    }
}
```

- [ ] **Step 2: Create `TodoScreen.kt`**

```kotlin
package com.michaelzhou.composedemo.todo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TodoScreen(viewModel: TodoViewModel = viewModel()) {
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).testTag("taskInput"),
                label = { Text("New task") }
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { viewModel.addTodo(input); input = "" },
                modifier = Modifier.testTag("addBtn")
            ) { Text("Add") }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("todoList"),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            items(todos, key = { it.id }) { todo ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .testTag("todoRow_${todo.text}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = todo.isDone,
                        onCheckedChange = { viewModel.toggleDone(todo.id) },
                        modifier = Modifier.testTag("cbDone_${todo.text}")
                    )
                    Text(
                        text = todo.text,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (todo.isDone) TextDecoration.LineThrough else null
                    )
                    TextButton(
                        onClick = { viewModel.deleteTodo(todo.id) },
                        modifier = Modifier.testTag("btnDelete_${todo.text}")
                    ) { Text("×") }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/michaelzhou/composedemo/todo/
git commit -m "feat(compose): add Todo ViewModel and Screen (#27)"
```

---

## Task 6: Todo Compose UI Tests (TC-004, TC-005, TC-006)

**Files:**
- Create: `app/src/androidTest/kotlin/com/michaelzhou/composedemo/TodoScreenTest.kt`

- [ ] **Step 1: Create `TodoScreenTest.kt`**

```kotlin
package com.michaelzhou.composedemo

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
```

- [ ] **Step 2: Run all 6 tests**

```bash
./gradlew connectedAndroidTest
```
Expected: 6 tests PASS, 0 failures

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/
git commit -m "test(compose): add Todo Compose UI tests TC-004~006 (#27)"
```

---

## Task 7: README

**Files:**
- Create: `android-testing-demo-compose/README.md`

- [ ] **Step 1: Create `README.md`**

```markdown
# Android Testing Demo — Compose (Compose UI Test)

Login + Todo two-screen app demonstrating Compose UI Test automation on Jetpack Compose.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM + StateFlow |
| Navigation | Navigation Compose |
| Testing | Compose UI Test (ui-test-junit4) |

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
git commit -m "docs(compose): add README with test instructions (#27)"
```
