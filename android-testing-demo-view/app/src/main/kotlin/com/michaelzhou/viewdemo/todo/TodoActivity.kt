package com.michaelzhou.viewdemo.todo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.michaelzhou.viewdemo.R

class TodoActivity : AppCompatActivity() {

    private val viewModel: TodoViewModel by viewModels {
        val email = intent.getStringExtra("user_email") ?: ""
        TodoViewModelFactory(application, email)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo)

        val etTask = findViewById<EditText>(R.id.etTask)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val rvTodos = findViewById<RecyclerView>(R.id.rvTodos)

        val adapter = TodoAdapter(
            onToggle = { viewModel.toggleDone(it) },
            onDelete = { viewModel.deleteTodo(it) }
        )
        rvTodos.layoutManager = LinearLayoutManager(this)
        rvTodos.adapter = adapter

        viewModel.todos.observe(this) { adapter.submitList(it) }

        btnAdd.setOnClickListener {
            viewModel.addTodo(etTask.text.toString())
            etTask.text.clear()
        }
    }
}
