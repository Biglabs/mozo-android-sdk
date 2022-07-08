package io.mozocoin.sdk

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.CallSuper
import io.mozocoin.sdk.utils.RuntimeLocaleChanger

open class MozoApp : Application() {

    @CallSuper
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(RuntimeLocaleChanger.wrapContext(base))
    }

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    @CallSuper
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        RuntimeLocaleChanger.overrideLocale(this)
    }

    companion object {
        @Volatile
        private lateinit var instance: MozoApp
    }
}