# ViewModel Unit Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add JUnit + Mockito unit tests for `LoginViewModel` and `TodoViewModel` in both Android demo projects, running on JVM without an emulator.

**Architecture:** ViewModels currently hardcode DAO retrieval via `AppDatabase.getInstance(application)`. To enable mocking, we add a secondary constructor accepting the DAO directly (testing backdoor). Tests use `mockito-kotlin` to mock DAOs and `kotlinx-coroutines-test` to control coroutine execution. An `ioDispatcher` parameter replaces the hardcoded `Dispatchers.IO` so tests run synchronously.

**Tech Stack:** JUnit 4, mockito-kotlin 5.x, kotlinx-coroutines-test 1.9.x, androidx.arch.core:core-testing (view project only for `InstantTaskExecutorRule`)

**Closes:** https://github.com/zhoujuxi2028/michael-zhou-portfolio/issues/39

---

## File Map

| Action | File |
|--------|------|
| Modify | `android-testing-demo-view/app/build.gradle.kts` |
| Modify | `android-testing-demo-view/app/src/main/kotlin/com/michaelzhou/viewdemo/login/LoginViewModel.kt` |
| Modify | `android-testing-demo-view/app/src/main/kotlin/com/michaelzhou/viewdemo/todo/TodoViewModel.kt` |
| Create | `android-testing-demo-view/app/src/test/kotlin/com/michaelzhou/viewdemo/login/LoginViewModelTest.kt` |
| Create | `android-testing-demo-view/app/src/test/kotlin/com/michaelzhou/viewdemo/todo/TodoViewModelTest.kt` |
| Modify | `android-testing-demo-compose/app/build.gradle.kts` |
| Modify | `android-testing-demo-compose/app/src/main/kotlin/com/michaelzhou/composedemo/login/LoginViewModel.kt` |
| Modify | `android-testing-demo-compose/app/src/main/kotlin/com/michaelzhou/composedemo/todo/TodoViewModel.kt` |
| Create | `android-testing-demo-compose/app/src/test/kotlin/com/michaelzhou/composedemo/login/LoginViewModelTest.kt` |
| Create | `android-testing-demo-compose/app/src/test/kotlin/com/michaelzhou/composedemo/todo/TodoViewModelTest.kt` |

---

### Task 0: Create feature branch

- [ ] **Step 1: Create and switch to feature branch**

```bash
git checkout -b feature/android-demo-viewmodel-tests
```

Expected: `Switched to a new branch 'feature/android-demo-viewmodel-tests'`

---

### Task 1: Add test dependencies — View project

**Files:**
- Modify: `android-testing-demo-view/app/build.gradle.kts`

- [ ] **Step 1: Add dependencies after the existing `testImplementation("junit:junit:4.13.2")` line**

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
testImplementation("androidx.arch.core:core-testing:2.2.0")
```

- [ ] **Step 2: Sync and verify no dependency conflicts**

```bash
cd android-testing-demo-view && ./gradlew dependencies --configuration testRuntimeClasspath | grep -E "(mockito|coroutines-test|core-testing)"
```

Expected: all three show up with their resolved versions.

- [ ] **Step 3: Commit**

```bash
git add android-testing-demo-view/app/build.gradle.kts
git commit -m "build(android-view): add mockito-kotlin + coroutines-test deps"
```

---

### Task 2: Refactor LoginViewModel — View project

**Files:**
- Modify: `android-testing-demo-view/app/src/main/kotlin/com/michaelzhou/viewdemo/login/LoginViewModel.kt`

Change from single constructor to: primary constructor takes `userDao` + `ioDispatcher`, secondary constructor (production path) takes only `Application`.

- [ ] **Step 1: Rewrite `LoginViewModel.kt`**

```kotlin
package com.michaelzhou.viewdemo.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.michaelzhou.viewdemo.db.AppDatabase
import com.michaelzhou.viewdemo.db.UserDao
import com.michaelzhou.viewdemo.utils.sha256
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel(
    application: Application,
    private val userDao: UserDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        AppDatabase.getInstance(application).userDao()
    )

    private var email = ""
    private var password = ""

    private val _isLoginEnabled = MutableLiveData(false)
    val isLoginEnabled: LiveData<Boolean> = _isLoginEnabled

    private val _loginError = MutableLiveData<String?>(null)
    val loginError: LiveData<String?> = _loginError

    private val _loginSuccess = MutableLiveData<String?>()
    val loginSuccess: LiveData<String?> = _loginSuccess

    fun onEmailChanged(value: String) {
        email = value
        _isLoginEnabled.value = email.isNotBlank() && password.isNotBlank()
    }

    fun onPasswordChanged(value: String) {
        password = value
        _isLoginEnabled.value = email.isNotBlank() && password.isNotBlank()
    }

    fun onLoginClicked() {
        viewModelScope.launch(ioDispatcher) {
            val user = userDao.findByEmail(email)
            if (user != null && user.passwordHash == sha256(password)) {
                _loginError.postValue(null)
                _loginSuccess.postValue(email)
            } else {
                _loginError.postValue("Invalid email or password")
            }
        }
    }
}
```

- [ ] **Step 2: Verify project still builds**

```bash
cd android-testing-demo-view && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add android-testing-demo-view/app/src/main/kotlin/com/michaelzhou/viewdemo/login/LoginViewModel.kt
git commit -m "refactor(android-view): inject UserDao + ioDispatcher into LoginViewModel"
```

---

### Task 3: Refactor TodoViewModel — View project

**Files:**
- Modify: `android-testing-demo-view/app/src/main/kotlin/com/michaelzhou/viewdemo/todo/TodoViewModel.kt`

- [ ] **Step 1: Rewrite `TodoViewModel.kt`**

```kotlin
package com.michaelzhou.viewdemo.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.michaelzhou.viewdemo.db.AppDatabase
import com.michaelzhou.viewdemo.db.TodoDao
import com.michaelzhou.viewdemo.db.TodoEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodoViewModel(
    application: Application,
    private val userEmail: String,
    private val todoDao: TodoDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    constructor(application: Application, userEmail: String) : this(
        application,
        userEmail,
        AppDatabase.getInstance(application).todoDao()
    )

    private val _todos = MutableLiveData<List<TodoItem>>(emptyList())
    val todos: LiveData<List<TodoItem>> = _todos

    init {
        loadTodos()
    }

    private fun loadTodos() {
        viewModelScope.launch(ioDispatcher) {
            val entities = todoDao.getTodosForUser(userEmail)
            _todos.postValue(entities.map { TodoItem(id = it.id, text = it.text, isDone = it.isDone) })
        }
    }

    fun addTodo(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            val id = todoDao.insert(TodoEntity(userEmail = userEmail, text = text))
            _todos.postValue(_todos.value.orEmpty() + TodoItem(id = id, text = text))
        }
    }

    fun toggleDone(id: Long) {
        val updated = _todos.value.orEmpty().map {
            if (it.id == id) it.copy(isDone = !it.isDone) else it
        }
        _todos.value = updated
        viewModelScope.launch(ioDispatcher) {
            val entity = todoDao.getTodosForUser(userEmail).find { it.id == id } ?: return@launch
            todoDao.update(entity.copy(isDone = !entity.isDone))
        }
    }

    fun deleteTodo(id: Long) {
        _todos.value = _todos.value.orEmpty().filter { it.id != id }
        viewModelScope.launch(ioDispatcher) { todoDao.deleteById(id) }
    }
}
```

- [ ] **Step 2: Verify project still builds**

```bash
cd android-testing-demo-view && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add android-testing-demo-view/app/src/main/kotlin/com/michaelzhou/viewdemo/todo/TodoViewModel.kt
git commit -m "refactor(android-view): inject TodoDao + ioDispatcher into TodoViewModel"
```

---

### Task 4: Write LoginViewModelTest — View project

**Files:**
- Create: `android-testing-demo-view/app/src/test/kotlin/com/michaelzhou/viewdemo/login/LoginViewModelTest.kt`

- [ ] **Step 1: Create the test file**

```kotlin
package com.michaelzhou.viewdemo.login

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.michaelzhou.viewdemo.db.UserDao
import com.michaelzhou.viewdemo.db.UserEntity
import com.michaelzhou.viewdemo.utils.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockApp: Application = mock()
    private val mockUserDao: UserDao = mock()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(mockApp, mockUserDao, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onLoginClicked_correctCredentials_postsSuccess() = runTest {
        whenever(mockUserDao.findByEmail("a@b.com")).thenReturn(
            UserEntity(email = "a@b.com", passwordHash = sha256("pass123"))
        )
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("pass123")
        viewModel.onLoginClicked()
        assertEquals("a@b.com", viewModel.loginSuccess.value)
        assertNull(viewModel.loginError.value)
    }

    @Test
    fun onLoginClicked_wrongPassword_postsError() = runTest {
        whenever(mockUserDao.findByEmail("a@b.com")).thenReturn(
            UserEntity(email = "a@b.com", passwordHash = sha256("correct"))
        )
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("wrong")
        viewModel.onLoginClicked()
        assertEquals("Invalid email or password", viewModel.loginError.value)
        assertNull(viewModel.loginSuccess.value)
    }

    @Test
    fun onLoginClicked_unregisteredEmail_postsError() = runTest {
        whenever(mockUserDao.findByEmail("nobody@test.com")).thenReturn(null)
        viewModel.onEmailChanged("nobody@test.com")
        viewModel.onPasswordChanged("any")
        viewModel.onLoginClicked()
        assertEquals("Invalid email or password", viewModel.loginError.value)
        assertNull(viewModel.loginSuccess.value)
    }

    @Test
    fun onEmailChanged_bothFieldsFilled_enablesLogin() {
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("pass")
        assertEquals(true, viewModel.isLoginEnabled.value)
    }

    @Test
    fun onPasswordChanged_emptyEmail_disablesLogin() {
        viewModel.onPasswordChanged("pass")
        assertEquals(false, viewModel.isLoginEnabled.value)
    }
}
```

- [ ] **Step 2: Run unit tests**

```bash
cd android-testing-demo-view && ./gradlew test --tests "com.michaelzhou.viewdemo.login.LoginViewModelTest"
```

Expected: 5 tests pass, BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add android-testing-demo-view/app/src/test/kotlin/com/michaelzhou/viewdemo/login/LoginViewModelTest.kt
git commit -m "test(android-view): add LoginViewModelTest (login success, wrong password, unregistered email)"
```

---

### Task 5: Write TodoViewModelTest — View project

**Files:**
- Create: `android-testing-demo-view/app/src/test/kotlin/com/michaelzhou/viewdemo/todo/TodoViewModelTest.kt`

- [ ] **Step 1: Create the test file**

```kotlin
package com.michaelzhou.viewdemo.todo

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.michaelzhou.viewdemo.db.TodoDao
import com.michaelzhou.viewdemo.db.TodoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockApp: Application = mock()
    private val mockTodoDao: TodoDao = mock()
    private lateinit var viewModel: TodoViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Mockito stub registration is not suspend — no runTest needed here
        whenever(mockTodoDao.getTodosForUser("user@test.com")).thenReturn(emptyList())
        viewModel = TodoViewModel(mockApp, "user@test.com", mockTodoDao, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addTodo_appendsItemToList() = runTest {
        whenever(mockTodoDao.insert(any())).thenReturn(1L)
        viewModel.addTodo("Buy milk")
        val todos = viewModel.todos.value.orEmpty()
        assertEquals(1, todos.size)
        assertEquals("Buy milk", todos[0].text)
        assertEquals(false, todos[0].isDone)
    }

    @Test
    fun addTodo_blankText_doesNotAppend() = runTest {
        viewModel.addTodo("   ")
        assertTrue(viewModel.todos.value.orEmpty().isEmpty())
    }

    @Test
    fun deleteTodo_removesItemFromList() = runTest {
        whenever(mockTodoDao.insert(any())).thenReturn(1L)
        viewModel.addTodo("Buy milk")
        viewModel.deleteTodo(1L)
        assertTrue(viewModel.todos.value.orEmpty().isEmpty())
    }

    @Test
    fun toggleDone_flipsIsDoneState() = runTest {
        whenever(mockTodoDao.insert(any())).thenReturn(1L)
        // Re-stub getTodosForUser so toggleDone's DAO query finds the entity.
        // Mockito uses last-registered stub: setUp used emptyList() for loadTodos(),
        // this stub applies to toggleDone's internal getTodosForUser call.
        whenever(mockTodoDao.getTodosForUser("user@test.com")).thenReturn(
            listOf(TodoEntity(id = 1L, userEmail = "user@test.com", text = "Buy milk", isDone = false))
        )
        viewModel.addTodo("Buy milk")
        viewModel.toggleDone(1L)
        val todos = viewModel.todos.value.orEmpty()
        assertEquals(true, todos[0].isDone)
    }

    @Test
    fun deleteTodo_callsDaoDeleteById() = runTest {
        whenever(mockTodoDao.insert(any())).thenReturn(1L)
        viewModel.addTodo("Buy milk")
        viewModel.deleteTodo(1L)
        verify(mockTodoDao).deleteById(1L)
    }
}
```

- [ ] **Step 2: Run unit tests**

```bash
cd android-testing-demo-view && ./gradlew test --tests "com.michaelzhou.viewdemo.todo.TodoViewModelTest"
```

Expected: 5 tests pass, BUILD SUCCESSFUL

- [ ] **Step 3: Run full unit test suite**

```bash
cd android-testing-demo-view && ./gradlew test
```

Expected: `HashUtilsTest` (4) + `LoginViewModelTest` (5) + `TodoViewModelTest` (5) = 14 tests pass

- [ ] **Step 4: Commit**

```bash
git add android-testing-demo-view/app/src/test/kotlin/com/michaelzhou/viewdemo/todo/TodoViewModelTest.kt
git commit -m "test(android-view): add TodoViewModelTest (add, delete, toggleDone)"
```

---

### Task 6: Add test dependencies — Compose project

**Files:**
- Modify: `android-testing-demo-compose/app/build.gradle.kts`

- [ ] **Step 1: Add dependencies after `testImplementation("junit:junit:4.13.2")`**

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
```

> Note: No `core-testing` needed — Compose project uses `StateFlow`, not `LiveData`.

- [ ] **Step 2: Sync and verify**

```bash
cd android-testing-demo-compose && ./gradlew dependencies --configuration testRuntimeClasspath | grep -E "(mockito|coroutines-test)"
```

Expected: both show up with resolved versions.

- [ ] **Step 3: Commit**

```bash
git add android-testing-demo-compose/app/build.gradle.kts
git commit -m "build(android-compose): add mockito-kotlin + coroutines-test deps"
```

---

### Task 7: Refactor LoginViewModel — Compose project

**Files:**
- Modify: `android-testing-demo-compose/app/src/main/kotlin/com/michaelzhou/composedemo/login/LoginViewModel.kt`

- [ ] **Step 1: Rewrite `LoginViewModel.kt`**

```kotlin
package com.michaelzhou.composedemo.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michaelzhou.composedemo.db.AppDatabase
import com.michaelzhou.composedemo.db.UserDao
import com.michaelzhou.composedemo.utils.sha256
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(
    application: Application,
    private val userDao: UserDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        AppDatabase.getInstance(application).userDao()
    )

    private val _email = MutableStateFlow("")
    private val _password = MutableStateFlow("")

    val isLoginEnabled: StateFlow<Boolean> = combine(_email, _password) { email, password ->
        email.isNotBlank() && password.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _loginSuccess = MutableStateFlow<String?>(null)
    val loginSuccess: StateFlow<String?> = _loginSuccess.asStateFlow()

    fun onEmailChanged(value: String) { _email.value = value }
    fun onPasswordChanged(value: String) { _password.value = value }

    fun onLoginClicked() {
        viewModelScope.launch(ioDispatcher) {
            val user = userDao.findByEmail(_email.value)
            if (user != null && user.passwordHash == sha256(_password.value)) {
                _loginError.value = null
                _loginSuccess.value = _email.value
            } else {
                _loginError.value = "Invalid email or password"
            }
        }
    }

    fun onLoginHandled() { _loginSuccess.value = null }
}
```

- [ ] **Step 2: Verify build**

```bash
cd android-testing-demo-compose && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add android-testing-demo-compose/app/src/main/kotlin/com/michaelzhou/composedemo/login/LoginViewModel.kt
git commit -m "refactor(android-compose): inject UserDao + ioDispatcher into LoginViewModel"
```

---

### Task 8: Refactor TodoViewModel — Compose project

**Files:**
- Modify: `android-testing-demo-compose/app/src/main/kotlin/com/michaelzhou/composedemo/todo/TodoViewModel.kt`

- [ ] **Step 1: Rewrite `TodoViewModel.kt`**

```kotlin
package com.michaelzhou.composedemo.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michaelzhou.composedemo.db.AppDatabase
import com.michaelzhou.composedemo.db.TodoDao
import com.michaelzhou.composedemo.db.TodoEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TodoViewModel(
    application: Application,
    private val userEmail: String,
    private val todoDao: TodoDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    constructor(application: Application, userEmail: String) : this(
        application,
        userEmail,
        AppDatabase.getInstance(application).todoDao()
    )

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    init {
        loadTodos()
    }

    private fun loadTodos() {
        viewModelScope.launch(ioDispatcher) {
            val entities = todoDao.getTodosForUser(userEmail)
            _todos.value = entities.map { TodoItem(id = it.id, text = it.text, isDone = it.isDone) }
        }
    }

    fun addTodo(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            val id = todoDao.insert(TodoEntity(userEmail = userEmail, text = text))
            _todos.value = _todos.value + TodoItem(id = id, text = text)
        }
    }

    fun toggleDone(id: Long) {
        _todos.value = _todos.value.map {
            if (it.id == id) it.copy(isDone = !it.isDone) else it
        }
        viewModelScope.launch(ioDispatcher) {
            val entity = todoDao.getTodosForUser(userEmail).find { it.id == id } ?: return@launch
            todoDao.update(entity.copy(isDone = !entity.isDone))
        }
    }

    fun deleteTodo(id: Long) {
        _todos.value = _todos.value.filter { it.id != id }
        viewModelScope.launch(ioDispatcher) { todoDao.deleteById(id) }
    }
}
```

- [ ] **Step 2: Verify build**

```bash
cd android-testing-demo-compose && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add android-testing-demo-compose/app/src/main/kotlin/com/michaelzhou/composedemo/todo/TodoViewModel.kt
git commit -m "refactor(android-compose): inject TodoDao + ioDispatcher into TodoViewModel"
```

---

### Task 9: Write LoginViewModelTest — Compose project

**Files:**
- Create: `android-testing-demo-compose/app/src/test/kotlin/com/michaelzhou/composedemo/login/LoginViewModelTest.kt`

> StateFlow values are readable directly (no `InstantTaskExecutorRule` needed). `isLoginEnabled` uses `combine()` with `SharingStarted.WhileSubscribed` — in unit tests this may not emit without a collector. Test it via `loginSuccess`/`loginError` instead, which are plain `MutableStateFlow`.

- [ ] **Step 1: Create the test file**

```kotlin
package com.michaelzhou.composedemo.login

import android.app.Application
import com.michaelzhou.composedemo.db.UserDao
import com.michaelzhou.composedemo.db.UserEntity
import com.michaelzhou.composedemo.utils.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockApp: Application = mock()
    private val mockUserDao: UserDao = mock()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(mockApp, mockUserDao, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onLoginClicked_correctCredentials_setsLoginSuccess() = runTest {
        whenever(mockUserDao.findByEmail("a@b.com")).thenReturn(
            UserEntity(email = "a@b.com", passwordHash = sha256("pass123"))
        )
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("pass123")
        viewModel.onLoginClicked()
        assertEquals("a@b.com", viewModel.loginSuccess.value)
        assertNull(viewModel.loginError.value)
    }

    @Test
    fun onLoginClicked_wrongPassword_setsError() = runTest {
        whenever(mockUserDao.findByEmail("a@b.com")).thenReturn(
            UserEntity(email = "a@b.com", passwordHash = sha256("correct"))
        )
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("wrong")
        viewModel.onLoginClicked()
        assertEquals("Invalid email or password", viewModel.loginError.value)
        assertNull(viewModel.loginSuccess.value)
    }

    @Test
    fun onLoginClicked_unregisteredEmail_setsError() = runTest {
        whenever(mockUserDao.findByEmail("nobody@test.com")).thenReturn(null)
        viewModel.onEmailChanged("nobody@test.com")
        viewModel.onPasswordChanged("any")
        viewModel.onLoginClicked()
        assertEquals("Invalid email or password", viewModel.loginError.value)
        assertNull(viewModel.loginSuccess.value)
    }

    @Test
    fun onLoginHandled_clearsLoginSuccess() = runTest {
        whenever(mockUserDao.findByEmail("a@b.com")).thenReturn(
            UserEntity(email = "a@b.com", passwordHash = sha256("pass"))
        )
        viewModel.onEmailChanged("a@b.com")
        viewModel.onPasswordChanged("pass")
        viewModel.onLoginClicked()
        viewModel.onLoginHandled()
        assertNull(viewModel.loginSuccess.value)
    }
}
```

- [ ] **Step 2: Run unit tests**

```bash
cd android-testing-demo-compose && ./gradlew test --tests "com.michaelzhou.composedemo.login.LoginViewModelTest"
```

Expected: 4 tests pass, BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add android-testing-demo-compose/app/src/test/kotlin/com/michaelzhou/composedemo/login/LoginViewModelTest.kt
git commit -m "test(android-compose): add LoginViewModelTest (login success, wrong password, unregistered email)"
```

---

### Task 10: Write TodoViewModelTest — Compose project

**Files:**
- Create: `android-testing-demo-compose/app/src/test/kotlin/com/michaelzhou/composedemo/todo/TodoViewModelTest.kt`

- [ ] **Step 1: Create the test file**

```kotlin
package com.michaelzhou.composedemo.todo

import android.app.Application
import com.michaelzhou.composedemo.db.TodoDao
import com.michaelzhou.composedemo.db.TodoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockApp: Application = mock()
    private val mockTodoDao: TodoDao = mock()
    private lateinit var viewModel: TodoViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Mockito stub registration is not suspend — no runTest needed here
        whenever(mockTodoDao.getTodosForUser("user@test.com")).thenReturn(emptyList())
        viewModel = TodoViewModel(mockApp, "user@test.com", mockTodoDao, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addTodo_appendsItemToList() = runTest {
        whenever(mockTodoDao.insert(any())).thenReturn(1L)
        viewModel.addTodo("Buy milk")
        assertEquals(1, viewModel.todos.value.size)
        assertEquals("Buy milk", viewModel.todos.value[0].text)
        assertEquals(false, viewModel.todos.value[0].isDone)
    }

    @Test
    fun addTodo_blankText_doesNotAppend() = runTest {
        viewModel.addTodo("   ")
        assertTrue(viewModel.todos.value.isEmpty())
    }

    @Test
    fun deleteTodo_removesItemFromList() = runTest {
        whenever(mockTodoDao.insert(any())).thenReturn(1L)
        viewModel.addTodo("Buy milk")
        viewModel.deleteTodo(1L)
        assertTrue(viewModel.todos.value.isEmpty())
    }

    @Test
    fun toggleDone_flipsIsDoneState() = runTest {
        whenever(mockTodoDao.insert(any())).thenReturn(1L)
        // Re-stub getTodosForUser so toggleDone's DAO query finds the entity.
        // Mockito uses last-registered stub: setUp used emptyList() for loadTodos(),
        // this stub applies to toggleDone's internal getTodosForUser call.
        whenever(mockTodoDao.getTodosForUser("user@test.com")).thenReturn(
            listOf(TodoEntity(id = 1L, userEmail = "user@test.com", text = "Buy milk", isDone = false))
        )
        viewModel.addTodo("Buy milk")
        viewModel.toggleDone(1L)
        assertEquals(true, viewModel.todos.value[0].isDone)
    }

    @Test
    fun deleteTodo_callsDaoDeleteById() = runTest {
        whenever(mockTodoDao.insert(any())).thenReturn(1L)
        viewModel.addTodo("Buy milk")
        viewModel.deleteTodo(1L)
        verify(mockTodoDao).deleteById(1L)
    }
}
```

- [ ] **Step 2: Run unit tests**

```bash
cd android-testing-demo-compose && ./gradlew test --tests "com.michaelzhou.composedemo.todo.TodoViewModelTest"
```

Expected: 5 tests pass, BUILD SUCCESSFUL

- [ ] **Step 3: Run full unit test suite**

```bash
cd android-testing-demo-compose && ./gradlew test
```

Expected: `HashUtilsTest` (4) + `LoginViewModelTest` (4) + `TodoViewModelTest` (5) = 13 tests pass

- [ ] **Step 4: Commit**

```bash
git add android-testing-demo-compose/app/src/test/kotlin/com/michaelzhou/composedemo/todo/TodoViewModelTest.kt
git commit -m "test(android-compose): add TodoViewModelTest (add, delete, toggleDone)"
```

---

### Task 11: Push + PR

- [ ] **Step 1: Verify both projects pass `./gradlew test`**

```bash
cd android-testing-demo-view && ./gradlew test
cd android-testing-demo-compose && ./gradlew test
```

Expected: all tests pass in both projects.

- [ ] **Step 2: Push and open PR**

```bash
git push -u origin feature/android-demo-viewmodel-tests
```

PR title: `test(android-demo): add ViewModel unit tests with JUnit + Mockito`
PR body: `Closes #39`

Expected: CI runs `./gradlew test` → picks up all new unit tests → all green.
