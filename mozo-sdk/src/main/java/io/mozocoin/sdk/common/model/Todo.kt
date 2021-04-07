package io.mozocoin.sdk.common.model

import io.mozocoin.sdk.common.TodoType

data class Todo(
        val id: String?,
        val data: TodoData?,
        val severity: String,
        val priority: Int?
) {
    fun type() = TodoType.find(id) ?: TodoType.CUSTOM
}