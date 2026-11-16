# TC-008 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add TC-008 UI test (unregistered email → error message) to both Android demo projects.

**Architecture:** TC-008 exercises the same `else` branch in `LoginViewModel.onLoginClicked()` as TC-007 (wrong password). No production code changes needed — just one new `@Test` method in each project's login test file.

**Tech Stack:** Espresso (view project), Compose UI Test (compose project), JUnit 4

**Closes:** https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/38

---

## File Map

| Action | File |
|--------|------|
| Modify | `android-testing-demo-view/app/src/androidTest/kotlin/com/michaelzhou/viewdemo/LoginActivityTest.kt` |
| Modify | `android-testing-demo-compose/app/src/androidTest/kotlin/com/michaelzhou/composedemo/LoginScreenTest.kt` |

---

### Task 0: Create feature branch

- [ ] **Step 1: Create and switch to feature branch**

```bash
git checkout -b feature/android-demo-tc008
```

Expected: `Switched to a new branch 'feature/android-demo-tc008'`

---

### Task 1: TC-008 — View project (Espresso)

**Files:**
- Modify: `android-testing-demo-view/app/src/androidTest/kotlin/com/michaelzhou/viewdemo/LoginActivityTest.kt`

The existing `@Before seedDb()` inserts `test@test.com`. TC-008 uses `nobody@test.com` which is not in the DB, so `userDao.findByEmail()` returns null → triggers error branch.

- [ ] **Step 1: Add test method to `LoginActivityTest.kt`**

Append after the existing `wrongPassword_showsErrorMessage` test:

```kotlin
// TC-008
@Test
fun unregisteredEmail_showsErrorMessage() {
    onView(withId(R.id.etEmail)).perform(typeText("nobody@test.com"), closeSoftKeyboard())
    onView(withId(R.id.etPassword)).perform(typeText("anypassword"), closeSoftKeyboard())
    onView(withId(R.id.btnLogin)).perform(click())
    onView(withId(R.id.tvLoginError)).check(matches(isDisplayed()))
    onView(withId(R.id.tvLoginError)).check(matches(withText("Invalid email or password")))
}
```

- [ ] **Step 2: Verify test count**

The file should now have 5 tests: TC-001, TC-002, TC-003, TC-007, TC-008.

- [ ] **Step 3: Commit**

```bash
git add android-testing-demo-view/app/src/androidTest/kotlin/com/michaelzhou/viewdemo/LoginActivityTest.kt
git commit -m "test(android-view): add TC-008 — unregistered email shows error"
```

---

### Task 2: TC-008 — Compose project (Compose UI Test)

**Files:**
- Modify: `android-testing-demo-compose/app/src/androidTest/kotlin/com/michaelzhou/composedemo/LoginScreenTest.kt`

Same logic — `nobody@test.com` not in DB → null user → error state → `loginError` node displayed.

- [ ] **Step 1: Add test method to `LoginScreenTest.kt`**

Append after `wrongPassword_showsErrorMessage`:

```kotlin
// TC-008
@Test
fun unregisteredEmail_showsErrorMessage() {
    composeRule.onNodeWithTag("emailField").performTextInput("nobody@test.com")
    composeRule.onNodeWithTag("passwordField").performTextInput("anypassword")
    composeRule.onNodeWithTag("loginBtn").performClick()
    composeRule.onNodeWithTag("loginError").assertIsDisplayed()
}
```

- [ ] **Step 2: Verify test count**

The file should now have 5 tests: TC-001, TC-002, TC-003, TC-007, TC-008.

- [ ] **Step 3: Commit**

```bash
git add android-testing-demo-compose/app/src/androidTest/kotlin/com/michaelzhou/composedemo/LoginScreenTest.kt
git commit -m "test(android-compose): add TC-008 — unregistered email shows error"
```

---

### Task 3: Verify locally + push + PR

UI tests require a connected emulator. Run either project manually to verify before PR.

- [ ] **Step 1: Run on emulator (optional local verification)**

```bash
# Start emulator first, then:
cd android-testing-demo-view && ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.michaelzhou.viewdemo.LoginActivityTest
cd android-testing-demo-compose && ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.michaelzhou.composedemo.LoginScreenTest
```

Expected: 5 tests pass in each project.

- [ ] **Step 2: Push branch and open PR**

```bash
git push -u origin feature/android-demo-tc008
```

PR title: `test(android-demo): add TC-008 — unregistered email login shows error`
PR body: `Closes #38`

> **Note:** CI runs `./gradlew test` (unit tests only). TC-008 is an `androidTest` (UI test) so CI won't execute it — that's expected. The PR demonstrates the test exists; local emulator run is the verification.
