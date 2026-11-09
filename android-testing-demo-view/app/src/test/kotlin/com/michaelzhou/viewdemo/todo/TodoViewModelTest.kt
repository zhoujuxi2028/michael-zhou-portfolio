package com.michaelzhou.viewdemo.todo

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.michaelzhou.viewdemo.db.TodoDao
import com.michaelzhou.viewdemo.db.TodoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
    fun setUp() = runBlocking {
        Dispatchers.setMain(testDispatcher)
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
