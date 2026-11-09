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
