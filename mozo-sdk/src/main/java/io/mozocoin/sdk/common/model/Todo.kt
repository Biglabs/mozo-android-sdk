package io.mozocoin.sdk.common.model

import io.mozocoin.sdk.common.TodoType

public data class Todo(
        val id: String?,
        val data: TodoData?,
        val severity: String,
        val priority: Int?
) {
    public fun idKey() = TodoType.find(id) ?: TodoType.LOCATION_SERVICE_OFF
}