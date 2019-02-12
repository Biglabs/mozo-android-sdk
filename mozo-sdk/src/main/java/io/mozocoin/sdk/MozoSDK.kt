package io.mozocoin.sdk

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
import io.mozocoin.sdk.common.OnNotificationReceiveListener
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.service.MozoDatabase
import io.mozocoin.sdk.common.service.MozoSocketClient
import io.mozocoin.sdk.utils.logAsInfo

class MozoSDK private constructor(internal val context: Context) : ViewModelStoreOwner {

    internal val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

    internal var onNotificationReceiveListener: OnNotificationReceiveListener? = null

    override fun getViewModelStore(): ViewModelStore = mViewModelStore

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network?) {
                if (MozoAuth.getInstance().isSignUpCompleted()) {
                    profileViewModel.fetchData(context)
                    contactViewModel.fetchData(context)
                }

                MozoSocketClient.connect()
            }

            override fun onLost(network: Network?) {
                "Network lost".logAsInfo()
                MozoSocketClient.disconnect()
            }
        })
    }

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
        internal var serviceEnvironment = ENVIRONMENT_STAGING

        @Volatile
        internal var isRetailerApp = false

        @Volatile
        internal var isEnableDebugLogging = false

        @JvmStatic
        @Synchronized
        fun initialize(context: Context, @Environment environment: Int = ENVIRONMENT_STAGING, isRetailerApp: Boolean = false) {
            if (instance == null) {
                checkNotNull(context)

                serviceEnvironment = environment
                Companion.isRetailerApp = isRetailerApp
                instance = MozoSDK(context.applicationContext)

                /* initialize Database Service */
                MozoDatabase.getInstance(context)
                /* initialize Authentication Service */
                MozoAuth.getInstance()
                /* initialize Wallet Service */
                MozoWallet.getInstance()
                /* initialize Transaction Service */
                MozoTx.getInstance()

                /* register network changes */
                instance?.registerNetworkCallback()
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
        fun attachNotificationReceiverActivity(activity: Class<out Activity>) {
            checkNotNull(activity)
            getInstance().notifyActivityClass = activity
        }

        internal fun isNetworkAvailable(): Boolean {
            val activeNetwork = getInstance().connectivityManager.activeNetworkInfo
            return activeNetwork?.isConnected == true
        }
    }
}