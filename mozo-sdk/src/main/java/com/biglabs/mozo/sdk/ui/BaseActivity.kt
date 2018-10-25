package com.biglabs.mozo.sdk.ui

import android.support.annotation.CallSuper
import android.support.v7.app.AppCompatActivity
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.common.MessageEvent
import kotlinx.android.synthetic.main.view_toolbar.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

internal open class BaseActivity : AppCompatActivity() {

    @CallSuper
    override fun onStart() {
        super.onStart()
        EventBus.getDefault()?.run {
            if (!isRegistered(this@BaseActivity))
                register(this@BaseActivity)
        }
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        MozoSDK.internalContext = this
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        MozoSDK.internalContext = null
        EventBus.getDefault().unregister(this)
    }

    override fun setTitle(titleId: Int) {
        screen_title?.apply {
            setText(titleId)
        }
        super.setTitle(titleId)
    }

    @Suppress("unused")
    @Subscribe
    fun onReceiveSignal(event: MessageEvent.CloseActivities) {
        finishAndRemoveTask()
    }
}