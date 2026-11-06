package com.michaelzhou.viewdemo.todo

data class TodoItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isDone: Boolean = false
)
