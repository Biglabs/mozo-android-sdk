package io.mozocoin.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.annotation.IntDef
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import io.mozocoin.sdk.common.ActivityLifecycleCallbacks
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.service.ConnectionService
import io.mozocoin.sdk.common.service.MozoTokenService
import io.mozocoin.sdk.ui.MaintenanceActivity
import io.mozocoin.sdk.ui.UpdateRequiredActivity
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.launchActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.greenrobot.eventbus.EventBus

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

    internal var ignoreInternetErrActivities: ArrayList<Class<out Activity>>? = null

    internal var remindAnchorView: View? = null

    internal var retryCallbacks: ArrayList<(() -> Unit)>? = null

    override fun getViewModelStore(): ViewModelStore = mViewModelStore

    private fun registerNetworkCallback() {
        val myJob = JobInfo.Builder(0, ComponentName(context, ConnectionService::class.java))
            .setMinimumLatency(2000)
            .setOverrideDeadline(2000)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            //.setPersisted(true)
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
        internal var isInternalApps = false

        @Volatile
        internal var isReadyForWallet = true

        @Volatile
        internal var isEnableDebugLogging = false

        @Volatile
        internal var hostName = ""

        @Volatile
        internal var shouldShowNotification = true

        internal val scope: CoroutineScope by lazy {
            CoroutineScope(Dispatchers.Default)
        }

        @JvmStatic
        @Synchronized
        fun initialize(
            context: Context,
            @Environment environment: Int = ENVIRONMENT_STAGING,
            useForBusiness: Boolean = false
        ) {
            if (instance == null) {
                hostName = context.packageName
                serviceEnvironment = environment
                isRetailerApp = useForBusiness
                isInternalApps = Support.isInternalApps(context)
                instance = MozoSDK(context.applicationContext)

                /**
                 * Report token
                 */
                MozoTokenService.instance().reportToken()

                // Preload custom tabs service for improved performance
                if (context is Application) {
                    context.registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks())
                }

                /**
                 * Initialize Authentication Service
                 * */
                MozoAuth.getInstance()

                /**
                 * Initialize Transaction Service
                 * */
                MozoTx.getInstance()
                /**
                 * Initialize Wallet Service
                 * */
                MozoWallet.getInstance()

                /**
                 * Register network changes
                 * */
                instance?.registerNetworkCallback()
            }
        }

        @JvmStatic
        @Synchronized
        fun getInstance(): MozoSDK {
            checkNotNull(instance) { "MozoSDK is not initialized. Make sure to call MozoSDK.initialize(Context) first." }
            return instance as MozoSDK
        }

        @JvmStatic
        fun attachNotificationReceiverActivity(activity: Class<out Activity>) {
            MozoNotification.getInstance().notifyActivityClass = activity
        }

        @JvmStatic
        fun contactTelegram(context: Context) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW, Uri.parse(
                        if (isRetailerApp) "https://t.me/MozoXRetailerApp"
                        else "https://t.me/MozoXApp"
                    )
                )
            )
        }

        @JvmStatic
        fun contactZalo(context: Context) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://zalo.me/4501855982660092369")
                )
            )
        }

        @JvmStatic
        fun contactKaKaoTalk(context: Context) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://open.kakao.com/o/g6tvra5")
                )
            )
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

        @JvmStatic
        fun startUpdateRequired(context: Context) {
            context.launchActivity<UpdateRequiredActivity> { }
        }

        @JvmStatic
        fun disableInternetFloatingBar(activity: Class<out Activity>) {
            if (getInstance().ignoreInternetErrActivities == null) {
                getInstance().ignoreInternetErrActivities = arrayListOf()
            }

            getInstance().ignoreInternetErrActivities?.add(activity)
        }

        @JvmStatic
        fun enableInternetFloatingBar(activity: Class<out Activity>) {
            getInstance().ignoreInternetErrActivities?.remove(activity)
        }
    }
}