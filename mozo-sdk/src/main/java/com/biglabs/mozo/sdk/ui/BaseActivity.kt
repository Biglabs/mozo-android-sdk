package com.biglabs.mozo.sdk.ui

import android.support.annotation.CallSuper
import android.support.v7.app.AppCompatActivity
import com.biglabs.mozo.sdk.MozoSDK

internal open class BaseActivity : AppCompatActivity() {

    @CallSuper
    override fun onResume() {
        super.onResume()
        MozoSDK.internalContext = this
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        MozoSDK.internalContext = null
    }
}