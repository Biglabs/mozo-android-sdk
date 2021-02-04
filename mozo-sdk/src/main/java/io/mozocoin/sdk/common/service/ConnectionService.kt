package io.mozocoin.sdk.common.service

import android.app.Service
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.view.View
import com.google.android.material.snackbar.Snackbar
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.ui.MozoSnackbar
import io.mozocoin.sdk.ui.dialog.ErrorDialog
import io.mozocoin.sdk.utils.logAsInfo

class ConnectionService : JobService() {

    private var networkIndex = -1

    private val connectivityManager: ConnectivityManager by lazy {
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (network.hashCode() >= networkIndex) {
                networkIndex = network.hashCode()
                onNetworkConnectionChanged(true)
            }
        }

        override fun onLost(network: Network) {
            if (network.hashCode() >= networkIndex) {
                networkIndex = network.hashCode()
                onNetworkConnectionChanged(false)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return Service.START_NOT_STICKY
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        val networkRequest = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        return true
    }

    interface ConnectionChangedListener {
        fun onConnectionChanged(isConnected: Boolean)
    }

    companion object {
        var isNetworkAvailable: Boolean = true
        private var listeners: ArrayList<ConnectionChangedListener>? = null

        private fun onNetworkConnectionChanged(isConnected: Boolean, resumeState: Boolean = true) {
            isNetworkAvailable = isConnected
            if (MozoSDK.getInstance().remindAnchorView != null) {
                if (isConnected) {
                    MozoSnackbar.dismiss()

                } else if (!ErrorDialog.isShowingForNetwork()) {
                    MozoSnackbar.make(
                            MozoSDK.getInstance().remindAnchorView!!,
                            R.string.mozo_notify_warning_internet,
                            Snackbar.LENGTH_INDEFINITE
                    ).setAction(R.string.mozo_notify_warning_action, View.OnClickListener {
                        it.context.startActivity(
                                Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                        )

                    }).show()
                }
            }

            if (isConnected) {
                "Network available".logAsInfo()
                if (!MozoAuth.getInstance().isInitialized) return
                ErrorDialog.retry()

                if (resumeState) {
                    MozoAuth.getInstance().isSignUpCompleted(MozoSDK.getInstance().context) {
                        if (!it) return@isSignUpCompleted
                        MozoSocketClient.connect()
                        MozoSDK.getInstance().contactViewModel.fetchData(MozoSDK.getInstance().context)
                    }
                }

            } else {
                "Network lost".logAsInfo()
                MozoSocketClient.disconnect()
            }

            listeners?.forEach {
                it.onConnectionChanged(isConnected)
            }
        }

        @Suppress("DEPRECATION")
        fun checkNetwork() {
            val cm = MozoSDK.getInstance().context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val isHasNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.activeNetwork != null
            } else cm.activeNetworkInfo != null

            onNetworkConnectionChanged(isHasNetwork, false)
        }

        fun addConnectionChangedListener(listener: ConnectionChangedListener) {
            if (listeners == null) {
                listeners = arrayListOf()
            }

            if (listeners?.contains(listener) == false) {
                listeners?.add(listener)
            }
        }

        fun removeConnectionChangeListener(listener: ConnectionChangedListener) {
            listeners?.remove(listener)
        }

        fun clearConnectionChangedListener() {
            listeners?.clear()
        }
    }
}