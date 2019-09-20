package io.mozocoin.sdk.ui

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mozocoin.sdk.MozoTodoList
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.TodoType
import io.mozocoin.sdk.common.model.Todo
import io.mozocoin.sdk.common.model.TodoSettings
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.mozoSetup
import io.mozocoin.sdk.utils.openTab
import io.mozocoin.sdk.wallet.backup.BackupWalletActivity
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_todo.*
import kotlinx.android.synthetic.main.item_todo.*
import kotlinx.android.synthetic.main.item_todo_header.*

internal class TodoActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    private val todoData = arrayListOf<Todo>()
    private val todoAdapter = TodoAdapter(this, todoData)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(false)
            setDisplayHomeAsUpEnabled(false)
        }

        todo_recycler_refresh?.apply {
            mozoSetup()
            setOnRefreshListener(this@TodoActivity)
        }

        todo_recycler?.apply {
            setHasFixedSize(true)
            adapter = todoAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        todo_recycler_refresh?.isRefreshing = true
        MozoTodoList.getInstance().fetchData(this, todoDataCallback)
        registerReceiver(onBluetoothStateChangedListener, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onPause() {
        unregisterReceiver(onBluetoothStateChangedListener)
        super.onPause()
    }

    override fun onRefresh() {
        MozoTodoList.getInstance().fetchData(this, todoDataCallback)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        MozoTodoList.getInstance().onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private val todoDataCallback: (TodoSettings, List<Todo>) -> Unit = { settings, data ->
        todoAdapter.updateSettings(settings)
        todoData.clear()
        todoData.addAll(data)

        todo_recycler?.adapter?.notifyDataSetChanged()
        todo_recycler_refresh?.isRefreshing = false

        todo_recycler_empty?.isVisible = todoData.isEmpty()
    }

    private val onBluetoothStateChangedListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent?.action, ignoreCase = true)) {
                onRefresh()
            }
        }
    }

    class TodoAdapter(val todoActivity: TodoActivity, val data: List<Todo>) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {
        private var todoSettings: TodoSettings? = null

        fun updateSettings(settings: TodoSettings?) {
            todoSettings = settings
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = data.size + 1

        override fun getItemViewType(position: Int): Int = when (position) {
            0 -> TYPE_HEADER
            else -> TYPE_ITEMS
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder = when (viewType) {
            TYPE_HEADER -> TodoHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_todo_header, parent, false))
            else -> TodoItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false))
        }

        override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
            when (position) {
                0 -> holder.bind(null)
                else -> holder.bind(data[position - 1])
            }
        }

        inner class TodoHeaderViewHolder(override val containerView: View) : TodoViewHolder(containerView) {
            override fun bind(d: Todo?) {
                val count = itemCount - 1
                val unsolved = if (count > 0) "($count)" else ""
                item_todo_total_unsolved?.text = itemView.context.getString(R.string.mozo_todo_unsolved, unsolved)
            }
        }

        inner class TodoItemViewHolder(override val containerView: View) : TodoViewHolder(containerView) {
            override fun bind(d: Todo?) {
                d ?: return

                val color = todoSettings?.colors?.get(d.severity ?: "") ?: "#969696"
                item_todo_container?.setBorderColor(Color.parseColor(color))
                item_todo_container_mask?.click {
                    handleItemClick(d.id ?: return@click)
                }

                TodoType.find(d.id)?.let {
                    item_todo_title?.setText(it.title)
                    item_todo_action?.setText(it.action)
                }
            }

            private fun handleItemClick(type: String) {
                when (type) {
                    TodoType.BLUETOOTH_OFF.name -> {
                        BluetoothAdapter.getDefaultAdapter()?.run {
                            if (!isEnabled) enable()
                        }
                    }
                    TodoType.LOCATION_SERVICE_OFF.name -> {
                        todoActivity.startActivity(
                                Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                        )
                    }
                    TodoType.LOW_MOZOX_RETAILER.name -> {
                        todoActivity.openTab("${Support.homePage()}/retailer-portal/buy-mozo-by-crypto")
                    }
                    TodoType.UNSECURE_WALLET.name -> {
                        todoActivity.startActivity(Intent(todoActivity, BackupWalletActivity::class.java))
                    }
                    else -> MozoTodoList.getInstance().listeners.map { l ->
                        l.onTodoItemClicked(todoActivity, type)
                    }
                }
            }
        }

        abstract inner class TodoViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
            abstract fun bind(d: Todo?)
        }

        companion object {
            private const val TYPE_HEADER = 0
            private const val TYPE_ITEMS = 1
        }
    }

    companion object {
        fun start(context: Context) {
            Intent(context, TodoActivity::class.java).apply {
                context.startActivity(this)
            }
        }
    }
}