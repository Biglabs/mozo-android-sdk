package io.mozocoin.sdk.common

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.common.service.NetworkSchedulerService
import io.mozocoin.sdk.utils.customtabs.CustomTabsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    private var customTabsHelper: CustomTabsHelper? = null

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        customTabsHelper = CustomTabsHelper()
    }

    override fun onActivityPaused(activity: Activity) {
        customTabsHelper?.unbindCustomTabsService(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        customTabsHelper?.bindCustomTabsService(activity)

        GlobalScope.launch(Dispatchers.Main) {
            delay(1000)

            if (!activity.isFinishing && !activity.isDestroyed) {
                MozoSDK.getInstance().remindAnchorView = activity.window.decorView
                NetworkSchedulerService.checkNetwork()
            }
        }
    }

    override fun onActivityStarted(activity: Activity?) {
    }

    override fun onActivityDestroyed(activity: Activity?) {
    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
    }

    override fun onActivityStopped(activity: Activity?) {
    }
}