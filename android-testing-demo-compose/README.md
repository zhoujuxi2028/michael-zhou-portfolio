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
