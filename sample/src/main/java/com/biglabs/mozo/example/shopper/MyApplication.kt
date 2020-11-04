package com.biglabs.mozo.example.shopper

import android.app.Application
import io.mozocoin.sdk.MozoSDK

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MozoSDK.initialize(this, MozoSDK.ENVIRONMENT_DEVELOP, useForBusiness = true)
        MozoSDK.attachNotificationReceiverActivity(MainActivity::class.java)
    }
}