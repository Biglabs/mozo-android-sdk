package com.biglabs.mozo.example.shopper

import android.app.Application
import com.biglabs.mozo.sdk.MozoSDK

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MozoSDK.initialize(this, MozoSDK.ENVIRONMENT_DEVELOP)
        MozoSDK.enableDebugLogging(true)
        MozoSDK.attachNotificationReceiverActivity(MainActivity::class.java)
    }
}