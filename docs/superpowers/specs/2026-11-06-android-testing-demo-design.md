# Design Spec: Android Testing Demo Apps

**Date**: 2026-11-06
**Issue**: [#27](https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/27)
**Status**: Design Approved

---

## Background

Issue #27 originally proposed adding Espresso tests to ClockApp. During requirements analysis, the ClockApp was found unsuitable for testing demos due to:

- Time updates every second → unstable UI assertions
- No user interaction → Espresso's click/input strengths unused
- ViewModel state not injectable → cannot mock time

**Decision**: Create two new independent Android apps that demonstrate Android testing capabilities comprehensively.

---

## Scope

Two new directories under `michael-zhou-portfolio/`:

```
michael-zhou-portfolio/
├── android-app/                      # existing ClockApp (unchanged)
├── android-testing-demo-compose/     # NEW: Compose + Compose UI Test
└── android-testing-demo-view/        # NEW: XML View + Espresso
```

---

## App Functionality (both apps, same UX)

### Screen 1 — Login

| Element | Behaviour |
|---------|-----------|
| Email field | Text input, required |
| Password field | Text input, required |
| Login button | Disabled when either field is empty; enabled when both filled |
| Error message | Shown when login attempted with empty fields |
| On success | Navigate to Todo screen |

### Screen 2 — Todo

| Element | Behaviour |
|---------|-----------|
| Task input field | Text input |
| Add button | Adds task to list |
| Task list | Shows all tasks |
| Checkbox per task | Toggles complete state; completed items show strikethrough |
| Delete button per task | Removes task from list |

---

## Architecture

### android-testing-demo-compose

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation Compose (NavHost) |
| State | ViewModel + StateFlow |
| Testing | Compose UI Test (`ui-test-junit4`) + `createAndroidComposeRule` |
| Min SDK | 29 (Android 10) |
| Kotlin | 1.9.25 |

**File structure**:
```
app/src/main/kotlin/com/michaelzhou/composedemo/
├── MainActivity.kt
├── navigation/
│   └── AppNavigation.kt
├── login/
│   ├── LoginScreen.kt
│   └── LoginViewModel.kt
└── todo/
    ├── TodoScreen.kt
    └── TodoViewModel.kt

app/src/androidTest/kotlin/com/michaelzhou/composedemo/
├── LoginScreenTest.kt
└── TodoScreenTest.kt
```

### android-testing-demo-view

| Layer | Technology |
|-------|-----------|
| UI | XML Layout + Material Design Components |
| Navigation | Intent (Activity-to-Activity) |
| State | ViewModel + LiveData |
| Testing | Espresso (`espresso-core`) + `ActivityScenarioRule` |
| Min SDK | 29 (Android 10) |
| Kotlin | 1.9.25 |

**File structure**:
```
app/src/main/kotlin/com/michaelzhou/viewdemo/
├── login/
│   ├── LoginActivity.kt
│   └── LoginViewModel.kt
└── todo/
    ├── TodoActivity.kt
    ├── TodoViewModel.kt
    └── TodoAdapter.kt

app/src/main/res/layout/
├── activity_login.xml
├── activity_todo.xml
└── item_todo.xml

app/src/androidTest/kotlin/com/michaelzhou/viewdemo/
├── LoginActivityTest.kt
└── TodoActivityTest.kt
```

---

## Test Cases (6 per app = 12 total)

| ID | Scenario | Testing Capability |
|----|----------|--------------------|
| TC-001 | Login button disabled when fields empty | UI state assertion |
| TC-002 | Login button enabled after filling both fields | Input event + state linkage |
| TC-003 | Successful login navigates to Todo screen | Navigation testing |
| TC-004 | Adding a task appears in the list | List operation |
| TC-005 | Checking a task marks it complete (strikethrough) | State toggle |
| TC-006 | Deleting a task removes it from list | Delete operation |

---

## Test Code Style

### Compose version (LoginScreenTest.kt)
```kotlin
@Test
fun loginButton_disabledWhenFieldsEmpty() {
    composeRule.onNodeWithTag("loginBtn").assertIsNotEnabled()
}

@Test
fun loginButton_enabledAfterFilling() {
    composeRule.onNodeWithTag("emailField").performTextInput("test@test.com")
    composeRule.onNodeWithTag("passwordField").performTextInput("123456")
    composeRule.onNodeWithTag("loginBtn").assertIsEnabled()
}
```

### View version (LoginActivityTest.kt)
```kotlin
@Test
fun loginButton_disabledWhenFieldsEmpty() {
    onView(withId(R.id.btnLogin)).check(matches(not(isEnabled())))
}

@Test
fun loginButton_enabledAfterFilling() {
    onView(withId(R.id.etEmail)).perform(typeText("test@test.com"))
    onView(withId(R.id.etPassword)).perform(typeText("123456"))
    onView(withId(R.id.btnLogin)).check(matches(isEnabled()))
}
```

---

## Out of Scope

- Backend / real authentication (use hardcoded mock credentials)
- Persistent storage (in-memory only)
- GitHub Actions CI (excluded per Issue #27 discussion)
- Unit tests (focus is on instrumented UI tests)

---

## Acceptance Criteria

- [ ] Both apps build successfully (`./gradlew assembleDebug`)
- [ ] All 12 test cases pass on emulator or real device
- [ ] Both READMEs document how to run tests
- [ ] Issue #27 updated to reflect new scope
