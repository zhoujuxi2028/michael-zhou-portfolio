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
