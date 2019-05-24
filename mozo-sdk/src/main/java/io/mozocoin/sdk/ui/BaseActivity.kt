package io.mozocoin.sdk.ui

import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import io.mozocoin.sdk.common.MessageEvent
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
    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun setTitle(titleId: Int) {
        screen_title?.apply {
            setText(titleId)
        }
        super.setTitle(titleId)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe
    fun onReceiveSignal(event: MessageEvent.CloseActivities) {
        finishAndRemoveTask()
    }
}