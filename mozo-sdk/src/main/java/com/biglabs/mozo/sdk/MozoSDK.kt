package com.biglabs.mozo.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import com.biglabs.mozo.sdk.auth.MozoAuth
import com.biglabs.mozo.sdk.services.WalletService


class MozoSDK private constructor() {

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

        @SuppressLint("StaticFieldLeak")
        @Volatile
        internal var internalContext: Context? = null

        @JvmStatic
        @Synchronized
        fun initialize(context: Context) {
            checkNotNull(context)
            this.context = context

            if (INSTANCE == null) {
                INSTANCE = MozoSDK()
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