package io.mozocoin.sdk.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mozocoin.sdk.MozoTodoList
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.TodoType
import io.mozocoin.sdk.common.model.Todo
import io.mozocoin.sdk.common.model.TodoSettings
import io.mozocoin.sdk.common.service.LocationService
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.isLocationPermissionGranted
import io.mozocoin.sdk.utils.mozoSetup
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_todo.*
import kotlinx.android.synthetic.main.item_todo.*
import kotlinx.android.synthetic.main.item_todo_header.*

internal class TodoActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val locationService: LocationService by lazy { LocationService.newInstance(this) }

    private var todoSettings: TodoSettings? = null
    private val todoData = arrayListOf<Todo>()
    private val todoAdapter = TodoAdapter(todoData)

    private var currentLocation: Location? = null

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

        locationService.setListener {
            currentLocation = it
            fetchData()

            if (currentLocation != null) {
                locationService.clearListener()
            }
        }

        getLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        fetchData()
    }

    override fun onRefresh() {
        fetchData()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationService.fetchLocation()

                } else {
                    getLocationPermission()
                }
            }
        }
    }

    private fun getLocationPermission() {
        if (!isLocationPermissionGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    )
            ) {
                /*
                AlertDialog.Builder(context ?: return)
                    .setTitle(R.string.text_location_permission_title)
                    .setMessage(R.string.text_location_permission_msg)
                    .setNegativeButton(R.string.text_location_permission_not_now, null)
                    .setPositiveButton(R.string.text_location_permission_settings) { _, _ ->
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context!!.packageName}")
                            startActivity(this)
                        }
                    }
                    .show()
                */
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSIONS_REQUEST_LOCATION
                )
            }
        }
    }

    private fun fetchData() {
        fun fetchSettings() {
            MozoAPIsService.getInstance().getTodoSettings(
                    this,
                    { data, _ ->
                        todoSettings = data
                        todoAdapter.updateSettings(todoSettings)

                    }, ::fetchSettings
            )
        }

        fun fetchTodo() {
            val isBluetoothOff = bluetoothAdapter?.state == BluetoothAdapter.STATE_OFF
                    || bluetoothAdapter?.state == BluetoothAdapter.STATE_TURNING_OFF
            MozoAPIsService.getInstance().getTodoList4Shopper(
                    this,
                    isBluetoothOff,
                    currentLocation?.latitude ?: 0.0,
                    currentLocation?.longitude ?: 0.0,
                    { data, _ ->
                        todoData.clear()
                        todoData.addAll(data?.items ?: emptyList())

                        todo_recycler?.adapter?.notifyDataSetChanged()
                        todo_recycler_refresh?.isRefreshing = false

                    }, ::fetchData)
        }

        if (todoSettings == null) fetchSettings()
        fetchTodo()
    }

    class TodoAdapter(val data: List<Todo>) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {
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
                item_todo_container?.click {
                    MozoTodoList.getInstance().listeners.map { l ->
                        l.onTodoItemClicked(d.id ?: return@map)
                    }
                }

                TodoType.find(d.id)?.let {
                    item_todo_title?.setText(it.title)
                    item_todo_action?.setText(it.action)
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
        private const val PERMISSIONS_REQUEST_LOCATION = 999

        fun start(context: Context) {
            Intent(context, TodoActivity::class.java).apply {
                context.startActivity(this)
            }
        }
    }
}