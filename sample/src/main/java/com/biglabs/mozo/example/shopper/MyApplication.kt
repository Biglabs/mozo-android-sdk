package com.biglabs.mozo.example.shopper

import io.mozocoin.sdk.MozoApp
import io.mozocoin.sdk.MozoSDK

class MyApplication : MozoApp() {

    override fun onCreate() {
        super.onCreate()

        MozoSDK.initialize(this, MozoSDK.ENVIRONMENT_DEVELOP)
        MozoSDK.attachNotificationReceiverActivity(MainActivity::class.java)
    }
}