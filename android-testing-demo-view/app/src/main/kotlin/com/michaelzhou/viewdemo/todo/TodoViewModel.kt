package com.michaelzhou.viewdemo.todo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TodoViewModel : ViewModel() {
    private val _todos = MutableLiveData<List<TodoItem>>(emptyList())
    val todos: LiveData<List<TodoItem>> = _todos

    fun addTodo(text: String) {
        if (text.isBlank()) return
        _todos.value = _todos.value.orEmpty() + TodoItem(text = text)
    }

    fun toggleDone(id: Long) {
        _todos.value = _todos.value.orEmpty().map {
            if (it.id == id) it.copy(isDone = !it.isDone) else it
        }
    }

    fun deleteTodo(id: Long) {
        _todos.value = _todos.value.orEmpty().filter { it.id != id }
    }
}
