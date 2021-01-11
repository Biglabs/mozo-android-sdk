package io.mozocoin.sdk.ui

import android.view.View
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.utils.adjustFontScale
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

internal open class BaseActivity : AppCompatActivity() {

    override fun onAttachedToWindow() {
        adjustFontScale()
        super.onAttachedToWindow()
    }

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

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    @CallSuper
    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(false)
            setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun setTitle(titleId: Int) {
        findViewById<TextView>(R.id.screen_title)?.apply {
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