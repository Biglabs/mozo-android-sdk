package com.biglabs.mozo.example.shopper

import android.app.Application
import com.biglabs.mozo.sdk.MozoSDK

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MozoSDK.initialize(this)
    }
}