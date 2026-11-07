package com.michaelzhou.composedemo.todo

data class TodoItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isDone: Boolean = false
)
