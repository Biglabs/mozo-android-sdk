package io.mozocoin.sdk.common.model

import android.content.Context
import io.mozocoin.sdk.common.TodoType

data class Todo(
        val id: String?,
        val data: TodoData?,
        val severity: String,
        val priority: Int?
) {
    fun displayTitle(context: Context) = when (val type = type()) {
        TodoType.CUSTOM -> {
            data?.customTitle
        }
        else -> {
            context.getString(type.title)
        }
    }

    fun displayAction(context: Context) = when (val type = type()) {
        TodoType.CUSTOM -> {
            data?.customAction
        }
        else -> {
            context.getString(type.action)
        }
    }

    fun type() = TodoType.find(id) ?: TodoType.CUSTOM
}