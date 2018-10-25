package com.biglabs.mozo.sdk

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.net.ConnectivityManager
import android.support.v4.app.FragmentActivity
import com.biglabs.mozo.sdk.common.ViewModels
import com.biglabs.mozo.sdk.core.WalletService

class MozoSDK private constructor(val profileViewModel: ViewModels.ProfileViewModel, val contactViewModel: ViewModels.ContactViewModel) {

    val auth: MozoAuth by lazy { MozoAuth.getInstance() }
    //val beacon: BeaconService by lazy { BeaconService.getInstance() }

    init {
        /* initialize Wallet Service */
        WalletService.getInstance()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: MozoSDK? = null

        @SuppressLint("StaticFieldLeak")
        @Volatile
        internal var context: Context? = null

        @Volatile
        internal var notifyActivityClass: Class<out FragmentActivity>? = null

        @SuppressLint("StaticFieldLeak")
        @Volatile
        internal var internalContext: Context? = null

        @JvmStatic
        @Synchronized
        fun initialize(activity: FragmentActivity) {
            checkNotNull(activity)

            notifyActivityClass = activity::class.java
            this.context = activity

            if (INSTANCE == null) {
                INSTANCE = MozoSDK(
                        ViewModelProviders.of(activity).get(ViewModels.ProfileViewModel::class.java),
                        ViewModelProviders.of(activity).get(ViewModels.ContactViewModel::class.java)
                )
            }
        }

        @JvmStatic
        @Synchronized
        fun getInstance(): MozoSDK {
            if (INSTANCE == null) {
                throw IllegalStateException("MozoSDK is not initialized. Make sure to call MozoSDK.initialize(Context) first.")
            }
            return INSTANCE as MozoSDK
        }

        internal fun currentContext() = internalContext ?: context

        internal fun isNetworkAvailable(): Boolean {
            val connectivityManager = currentContext()?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }
}