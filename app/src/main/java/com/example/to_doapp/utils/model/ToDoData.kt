package com.example.to_doapp.utils.model

data class ToDoData(
    val taskId: String,
    var task: String,
    val timestamp: Long = System.currentTimeMillis(),
    var isSelected: Boolean = false,
    var isPinned: Boolean = false
)

