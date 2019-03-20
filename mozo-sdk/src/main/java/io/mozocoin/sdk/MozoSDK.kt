package io.mozocoin.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.Uri
import androidx.annotation.IntDef
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.OnNotificationReceiveListener
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.service.MozoDatabase
import io.mozocoin.sdk.common.service.MozoSocketClient
import io.mozocoin.sdk.utils.logAsInfo
import java.util.*

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
                if (!MozoAuth.getInstance().isInitialized) return

                MozoAuth.getInstance().isSignUpCompleted {
                    if (!it) return@isSignUpCompleted
                    MozoSocketClient.connect()
                    contactViewModel.fetchData(context)
                }
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

        @Volatile
        internal var shouldShowNotification = true

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
                /* initialize Wallet Service */
                MozoWallet.getInstance()
                /* initialize Transaction Service */
                MozoTx.getInstance()

                /* register network changes */
                instance?.registerNetworkCallback()

                context.registerComponentCallbacks(object : ComponentCallbacks {
                    override fun onLowMemory() {

                    }

                    override fun onConfigurationChanged(newConfig: Configuration?) {
                        newConfig ?: return

                        val symbol = getInstance().profileViewModel
                                .exchangeRateLiveData.value?.currencySymbol ?: return
                        if (
                                when (Locale.getDefault().language) {
                                    Locale.KOREA.language -> symbol != Constant.CURRENCY_SYMBOL_KRW
                                    Locale("vi").language -> symbol != Constant.CURRENCY_SYMBOL_VND
                                    else -> symbol != Constant.DEFAULT_CURRENCY_SYMBOL
                                }
                        ) {
                            getInstance().profileViewModel.fetchExchangeRate(context)
                        }
                    }
                })
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

        @JvmStatic
        fun shouldShowNotification(show: Boolean) {
            shouldShowNotification = show
        }

        internal fun isNetworkAvailable(): Boolean {
            val activeNetwork = getInstance().connectivityManager.activeNetworkInfo
            return activeNetwork?.isConnected == true
        }

        @JvmStatic
        fun contactTelegram(context: Context) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/mozotoken")))
        }

        @JvmStatic
        fun contactZalo(context: Context) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/428563224447178063")))
        }

        @JvmStatic
        fun contactKaKaoTalk(context: Context) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open.kakao.com/o/g6tvra5")))
        }
    }
}