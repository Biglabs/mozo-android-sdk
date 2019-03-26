package io.mozocoin.sdk.utils.customtabs

import android.app.Activity
import android.app.Application
import android.os.Bundle

class CustomTabsActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    private var customTabsHelper: CustomTabsHelper? = null

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        customTabsHelper = CustomTabsHelper()
    }

    override fun onActivityPaused(activity: Activity) {
        customTabsHelper?.unbindCustomTabsService(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        customTabsHelper?.bindCustomTabsService(activity)
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