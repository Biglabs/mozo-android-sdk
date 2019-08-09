package io.mozocoin.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentCallbacks
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.view.View
import androidx.annotation.IntDef
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.OnNotificationReceiveListener
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.service.MozoDatabase
import io.mozocoin.sdk.common.service.NetworkSchedulerService
import io.mozocoin.sdk.ui.MaintenanceActivity
import io.mozocoin.sdk.utils.customtabs.CustomTabsActivityLifecycleCallbacks
import org.greenrobot.eventbus.EventBus
import java.util.*

class MozoSDK private constructor(internal val context: Context) : ViewModelStoreOwner {

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

    internal var remindAnchorView: View? = null

    internal var onNotificationReceiveListener: OnNotificationReceiveListener? = null

    internal var retryCallbacks: ArrayList<(() -> Unit)>? = null

    override fun getViewModelStore(): ViewModelStore = mViewModelStore

    private fun registerNetworkCallback() {
        val myJob = JobInfo.Builder(0, ComponentName(context, NetworkSchedulerService::class.java))
                .setRequiresCharging(true)
                .setMinimumLatency(1000)
                .setOverrideDeadline(2000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
//                .setPersisted(true)
                .build()

        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(myJob)
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
        internal var isReadyForWallet = true

        @Volatile
        internal var isEnableDebugLogging = false

        @Volatile
        internal var shouldShowNotification = true

        @JvmStatic
        @Synchronized
        fun initialize(context: Context, @Environment environment: Int = ENVIRONMENT_STAGING, isRetailerApp: Boolean = false) {
            if (instance == null) {
                serviceEnvironment = environment
                Companion.isRetailerApp = isRetailerApp
                instance = MozoSDK(context.applicationContext)

                /* initialize Database Service */
                MozoDatabase.getInstance(context)
                /* initialize Wallet Service */
                MozoWallet.getInstance()
                /* initialize Transaction Service */
                MozoTx.getInstance()

                // Preload custom tabs service for improved performance
                if (context is Application) {
                    context.registerActivityLifecycleCallbacks(CustomTabsActivityLifecycleCallbacks())
                }

                /* register network changes */
                instance?.registerNetworkCallback()

                context.registerComponentCallbacks(object : ComponentCallbacks {
                    override fun onLowMemory() {

                    }

                    override fun onConfigurationChanged(newConfig: Configuration?) {
                        newConfig ?: return

                        val symbol = getInstance().profileViewModel
                                .exchangeRateLiveData.value?.token?.currencySymbol
                                ?: Constant.DEFAULT_CURRENCY_SYMBOL
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
        fun attachNotificationReceiverActivity(activity: Class<out Activity>) {
            getInstance().notifyActivityClass = activity
        }

        @JvmStatic
        fun attachViewForRemindSystem(anchorView: View) {
            getInstance().remindAnchorView = anchorView
        }

        @JvmStatic
        fun contactTelegram(context: Context) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                    if (isRetailerApp) "https://t.me/MozoXRetailerApp"
                    else "https://t.me/MozoXApp"
            )))
        }

        @JvmStatic
        fun contactZalo(context: Context) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/428563224447178063")))
        }

        @JvmStatic
        fun contactKaKaoTalk(context: Context) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open.kakao.com/o/g6tvra5")))
        }

        @JvmStatic
        fun enableDebugLogging(enable: Boolean) {
            isEnableDebugLogging = enable
        }

        @JvmStatic
        fun shouldShowNotification(show: Boolean) {
            shouldShowNotification = show
        }

        @JvmStatic
        fun readyForWallet(isReady: Boolean) {
            isReadyForWallet = isReady
        }

        @JvmStatic
        fun startMaintenanceMode(context: Context) {
            context.startActivity(Intent(context, MaintenanceActivity::class.java))
        }

        @JvmStatic
        fun stopMaintenanceMode() {
            EventBus.getDefault().post(MessageEvent.StopMaintenanceMode())
        }
    }
}