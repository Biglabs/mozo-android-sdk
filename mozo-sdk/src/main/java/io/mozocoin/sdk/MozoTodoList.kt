package io.mozocoin.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import io.mozocoin.sdk.common.model.Todo
import io.mozocoin.sdk.common.model.TodoData
import io.mozocoin.sdk.common.model.TodoSettings
import io.mozocoin.sdk.common.service.LocationService
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.ui.TodoActivity
import io.mozocoin.sdk.utils.isLocationPermissionGranted
import java.util.concurrent.CountDownLatch

class MozoTodoList private constructor() {

    internal val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = MozoSDK.getInstance().context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager
        bluetoothManager.adapter
    }
    private val locationService: LocationService by lazy { LocationService.newInstance(MozoSDK.getInstance().context) }

    private var countDownLatch: CountDownLatch? = null
    private var currentLocation: Location? = null
    private var todoSettings: TodoSettings? = null
    private var todoData: List<Todo>? = null

    private var lastActivity: Activity? = null
    private var lastCallback: ((TodoSettings, List<Todo>) -> Unit)? = null

    internal var listeners: ArrayList<TodoInteractListener> = arrayListOf()
    private var todoFinishListeners: ArrayList<TodoFinishListener> = arrayListOf()

    init {
        locationService.setListener {
            currentLocation = it
            if (lastActivity != null) {
                fetchData(lastActivity!!, lastCallback)
            }

            if (currentLocation != null) {
                lastActivity = null
                lastCallback = null
                locationService.clearListener()
            }
        }
    }

    internal fun fetchTodo() {
        lastActivity ?: return
        val isBluetoothOff = bluetoothAdapter.state == BluetoothAdapter.STATE_OFF
                || bluetoothAdapter.state == BluetoothAdapter.STATE_TURNING_OFF
        MozoAPIsService.getInstance().getTodoList4Shopper(
            lastActivity!!,
            isBluetoothOff,
            currentLocation?.latitude ?: 0.0,
            currentLocation?.longitude ?: 0.0,
            { data, _ ->
                countDownLatch?.countDown()
                todoData = data?.items

                if (todoData != null) {
                    val itemHighLight = todoData?.maxByOrNull { it.priority ?: 0 }
                    listeners.forEach {
                        it.onTodoTotalChanged(
                            todoData!!.size,
                            if ((itemHighLight?.priority ?: 0) > 0) itemHighLight else null
                        )
                    }
                }

                checkCountDown(lastCallback)
            }, {
                fetchData(lastActivity!!, lastCallback)
            })
    }

    fun fetchData(activity: Activity, callback: ((TodoSettings, List<Todo>) -> Unit)? = null) {
        if (!getLocationPermission(activity)) return
        lastActivity = activity
        lastCallback = callback

        countDownLatch = CountDownLatch(2)

        fun fetchSettings() {
            MozoAPIsService.getInstance().getTodoSettings(
                activity,
                { data, _ ->
                    countDownLatch?.countDown()
                    todoSettings = data
                    checkCountDown(callback)

                }, ::fetchSettings
            )
        }

        if (todoSettings == null) fetchSettings()
        else countDownLatch?.countDown()

        fetchTodo()
    }

    private fun checkCountDown(callback: ((TodoSettings, List<Todo>) -> Unit)?) {
        if ((countDownLatch?.count ?: 0) == 0L) {
            callback?.invoke(todoSettings ?: return, todoData ?: emptyList())
        }
    }

    fun open(context: Context) {
        TodoActivity.start(context)
    }

    fun close() {
        todoFinishListeners.forEach { it.onRequestFinish() }
        todoFinishListeners.clear()
    }

    @Suppress("unused")
    fun addListener(l: TodoInteractListener) {
        listeners.add(l)
    }

    @Suppress("unused")
    fun removeListener(l: TodoInteractListener) {
        listeners.remove(l)
    }

    fun registerTodoFinishListener(listener: TodoFinishListener) {
        todoFinishListeners.add(listener)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationService.fetchLocation()

                }
            }
        }
    }

    private fun getLocationPermission(activity: Activity) =
        if (!activity.isLocationPermissionGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
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
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_LOCATION
                )
            }

            false

        } else true

    interface TodoInteractListener {
        fun onTodoTotalChanged(total: Int, itemHighLight: Todo?)
        fun onTodoItemClicked(type: String, data: TodoData?)
    }

    interface TodoFinishListener {
        fun onRequestFinish()
    }

    companion object {
        private const val PERMISSIONS_REQUEST_LOCATION = 999

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: MozoTodoList? = null

        @JvmStatic
        fun getInstance() = instance ?: synchronized(this) {
            if (instance == null) instance = MozoTodoList()
            instance
        }!!
    }
}