package com.biglabs.mozo.sdk

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.support.v4.app.FragmentActivity
import com.biglabs.mozo.sdk.common.ViewModels
import com.biglabs.mozo.sdk.core.MozoSocketClient
import com.biglabs.mozo.sdk.core.WalletService

class MozoSDK private constructor(val profileViewModel: ViewModels.ProfileViewModel, val contactViewModel: ViewModels.ContactViewModel) {

    internal val connectivityManager: ConnectivityManager by lazy { context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network?) {
            context?.run {
                if (MozoAuth.getInstance().isSignUpCompleted()) {
                    profileViewModel.fetchData(this)
                    contactViewModel.fetchData(this)
                }

                MozoSocketClient.connect(this)
            }
        }

        override fun onLost(network: Network?) {
            MozoSocketClient.disconnect()
        }
    }

    init {
        /* initialize Wallet Service */
        WalletService.getInstance()

        /* register network changes */
        val networkRequest = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: MozoSDK? = null

        @SuppressLint("StaticFieldLeak")
        @Volatile
        internal var context: Context? = null

        @Volatile
        internal var notifyActivityClass: Class<out FragmentActivity>? = null

        @JvmStatic
        @Synchronized
        fun initialize(activity: FragmentActivity) {
            checkNotNull(activity)

            notifyActivityClass = activity::class.java
            this.context = activity.applicationContext

            if (instance == null) {
                instance = MozoSDK(
                        ViewModelProviders.of(activity).get(ViewModels.ProfileViewModel::class.java),
                        ViewModelProviders.of(activity).get(ViewModels.ContactViewModel::class.java)
                )
            }
        }

        @JvmStatic
        @Synchronized
        fun getInstance(): MozoSDK {
            if (instance == null) {
                throw IllegalStateException("MozoSDK is not initialized. Make sure to call MozoSDK.initialize(Context) first.")
            }
            return instance as MozoSDK
        }

        internal fun isNetworkAvailable(): Boolean {
            val activeNetwork = MozoSDK.getInstance().connectivityManager.activeNetworkInfo
            return activeNetwork?.isConnected == true
        }
    }
}