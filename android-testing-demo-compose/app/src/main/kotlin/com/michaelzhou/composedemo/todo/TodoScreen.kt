package com.michaelzhou.composedemo.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TodoScreen(
    userEmail: String = "",
    viewModel: TodoViewModel = viewModel(
        factory = TodoViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application,
            userEmail
        )
    )
) {
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).testTag("taskInput"),
                label = { Text("New task") }
            )
            Button(
                onClick = { viewModel.addTodo(input); input = "" },
                modifier = Modifier.testTag("addBtn")
            ) { Text("Add") }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("todoList"),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            items(todos, key = { it.id }) { todo ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .testTag("todoRow_${todo.text}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = todo.isDone,
                        onCheckedChange = { viewModel.toggleDone(todo.id) },
                        modifier = Modifier.testTag("cbDone_${todo.text}")
                    )
                    Text(
                        text = todo.text,
                        modifier = Modifier.weight(1f),
                        textDecoration = if (todo.isDone) TextDecoration.LineThrough else null
                    )
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = { viewModel.deleteTodo(todo.id) },
                        modifier = Modifier.testTag("btnDelete_${todo.text}")
                    ) { Text("×") }
                }
            }
        }
    }
}
