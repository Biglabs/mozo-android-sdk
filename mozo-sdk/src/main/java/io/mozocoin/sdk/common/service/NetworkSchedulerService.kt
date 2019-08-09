package io.mozocoin.sdk.common.service

import android.app.Service
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.google.android.material.snackbar.Snackbar
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.ui.MozoSnackbar
import io.mozocoin.sdk.ui.dialog.ErrorDialog
import io.mozocoin.sdk.utils.logAsInfo

class NetworkSchedulerService : JobService() {

    private val connectivityManager: ConnectivityManager by lazy {
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network?) {
            onNetworkConnectionChanged(true)
        }

        override fun onLost(network: Network?) {
            onNetworkConnectionChanged(false)
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

    private fun onNetworkConnectionChanged(isConnected: Boolean) {
        if (MozoSDK.getInstance().remindAnchorView != null) {
            MozoSnackbar.make(
                    MozoSDK.getInstance().remindAnchorView!!,
                    if (isConnected) "Good! Connected to Internet" else "Sorry! Not connected to internet",
                    Snackbar.LENGTH_INDEFINITE
            ).show()
        }

        if (isConnected) {
            "Network available".logAsInfo()
            ErrorDialog.retry()
            if (!MozoAuth.getInstance().isInitialized) return

            MozoAuth.getInstance().isSignUpCompleted(MozoSDK.getInstance().context) {
                if (!it) return@isSignUpCompleted
                MozoSocketClient.connect()
                MozoSDK.getInstance().contactViewModel.fetchData(MozoSDK.getInstance().context)
            }

        } else {
            "Network lost".logAsInfo()
            MozoSocketClient.disconnect()
        }
    }

    companion object {
        internal fun isNetworkAvailable(): Boolean {
            val cm = MozoSDK.getInstance().context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo?.isConnected == true
        }
    }
}