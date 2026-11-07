package com.michaelzhou.viewdemo.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.michaelzhou.viewdemo.db.AppDatabase
import com.michaelzhou.viewdemo.db.TodoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodoViewModel(application: Application, private val userEmail: String) :
    AndroidViewModel(application) {

    private val todoDao = AppDatabase.getInstance(application).todoDao()

    private val _todos = MutableLiveData<List<TodoItem>>(emptyList())
    val todos: LiveData<List<TodoItem>> = _todos

    init {
        loadTodos()
    }

    private fun loadTodos() {
        viewModelScope.launch(Dispatchers.IO) {
            val entities = todoDao.getTodosForUser(userEmail)
            _todos.postValue(entities.map { TodoItem(id = it.id, text = it.text, isDone = it.isDone) })
        }
    }

    fun addTodo(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val id = todoDao.insert(TodoEntity(userEmail = userEmail, text = text))
            _todos.postValue(_todos.value.orEmpty() + TodoItem(id = id, text = text))
        }
    }

    fun toggleDone(id: Long) {
        val updated = _todos.value.orEmpty().map {
            if (it.id == id) it.copy(isDone = !it.isDone) else it
        }
        _todos.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            val entity = todoDao.getTodosForUser(userEmail).find { it.id == id } ?: return@launch
            todoDao.update(entity.copy(isDone = !entity.isDone))
        }
    }

    fun deleteTodo(id: Long) {
        _todos.value = _todos.value.orEmpty().filter { it.id != id }
        viewModelScope.launch(Dispatchers.IO) { todoDao.deleteById(id) }
    }
}
