package com.biglabs.mozo.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.annotation.IntDef
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.biglabs.mozo.sdk.common.ViewModels
import com.biglabs.mozo.sdk.core.MozoSocketClient

class MozoSDK private constructor() : ViewModelStoreOwner {

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

    private val mViewModelStore: ViewModelStore by lazy { ViewModelStore() }
    internal val profileViewModel: ViewModels.ProfileViewModel by lazy {
        ViewModelProvider(this, ViewModelProvider.NewInstanceFactory())
                .get(ViewModels.ProfileViewModel::class.java)
    }

    internal val contactViewModel: ViewModels.ContactViewModel by lazy {
        ViewModelProvider(this, ViewModelProvider.NewInstanceFactory())
                .get(ViewModels.ContactViewModel::class.java)
    }

    internal var notifyActivityClass: Class<out Activity>? = null

    init {
        /* initialize Wallet Service */
        MozoWallet.getInstance()

        /* register network changes */
        val networkRequest = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun getViewModelStore(): ViewModelStore = mViewModelStore

    @Suppress("unused")
    companion object {
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(ENVIRONMENT_DEVELOP, ENVIRONMENT_STAGING, ENVIRONMENT_PRODUCTION)
        annotation class Environment

        const val ENVIRONMENT_PRODUCTION = 0
        const val ENVIRONMENT_STAGING = 1
        const val ENVIRONMENT_DEVELOP = 2

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: MozoSDK? = null

        @Volatile
        internal var serviceEnvironment = ENVIRONMENT_PRODUCTION

        @SuppressLint("StaticFieldLeak")
        @Volatile
        internal var context: Context? = null

        @Volatile
        internal var isEnableDebugLogging = false

        @JvmStatic
        @Synchronized
        fun initialize(context: Context, @Environment environment: Int = ENVIRONMENT_PRODUCTION) {
            if (instance == null) {
                checkNotNull(context)

                this.serviceEnvironment = environment
                this.context = context.applicationContext
                this.instance = MozoSDK()
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

        @JvmStatic
        fun enableDebugLogging(enable: Boolean) {
            isEnableDebugLogging = enable
        }

        @JvmStatic
        fun attachActivityToReceiveNotificationMessage(activity: Class<out Activity>) {
            checkNotNull(activity)
            getInstance().notifyActivityClass = activity
        }

        internal fun isNetworkAvailable(): Boolean {
            val activeNetwork = MozoSDK.getInstance().connectivityManager.activeNetworkInfo
            return activeNetwork?.isConnected == true
        }
    }
}