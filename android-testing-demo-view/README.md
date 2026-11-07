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
