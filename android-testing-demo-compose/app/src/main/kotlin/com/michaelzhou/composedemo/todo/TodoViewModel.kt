package com.michaelzhou.composedemo.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michaelzhou.composedemo.db.AppDatabase
import com.michaelzhou.composedemo.db.TodoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TodoViewModel(application: Application, private val userEmail: String) :
    AndroidViewModel(application) {

    private val todoDao = AppDatabase.getInstance(application).todoDao()

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    init {
        loadTodos()
    }

    private fun loadTodos() {
        viewModelScope.launch(Dispatchers.IO) {
            val entities = todoDao.getTodosForUser(userEmail)
            _todos.value = entities.map { TodoItem(id = it.id, text = it.text, isDone = it.isDone) }
        }
    }

    fun addTodo(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val id = todoDao.insert(TodoEntity(userEmail = userEmail, text = text))
            _todos.value = _todos.value + TodoItem(id = id, text = text)
        }
    }

    fun toggleDone(id: Long) {
        _todos.value = _todos.value.map {
            if (it.id == id) it.copy(isDone = !it.isDone) else it
        }
        viewModelScope.launch(Dispatchers.IO) {
            val entity = todoDao.getTodosForUser(userEmail).find { it.id == id } ?: return@launch
            todoDao.update(entity.copy(isDone = !entity.isDone))
        }
    }

    fun deleteTodo(id: Long) {
        _todos.value = _todos.value.filter { it.id != id }
        viewModelScope.launch(Dispatchers.IO) { todoDao.deleteById(id) }
    }
}
