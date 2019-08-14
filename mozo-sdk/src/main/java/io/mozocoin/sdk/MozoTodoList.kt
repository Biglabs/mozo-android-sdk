package io.mozocoin.sdk

import android.content.Context
import io.mozocoin.sdk.ui.TodoActivity

class MozoTodoList private constructor() {

    internal var listeners: ArrayList<TodoInteractListener> = arrayListOf()

    fun open(context: Context) {
        TodoActivity.start(context)
    }

    fun addListener(l: TodoInteractListener) {
        listeners.add(l)
    }

    fun removeListener(l: TodoInteractListener) {
        listeners.remove(l)
    }

    abstract inner class TodoInteractListener {
        fun onTodoTotalChanged(total: Int) {

        }

        fun onTodoItemClicked(type: String) {

        }
    }

    companion object {
        @Volatile
        private var instance: MozoTodoList? = null

        @JvmStatic
        fun getInstance() = instance ?: synchronized(this) {
            if (instance == null) instance = MozoTodoList()
            instance
        }!!
    }
}