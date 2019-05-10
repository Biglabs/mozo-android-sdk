package io.mozocoin.sdk.ui

import android.os.Bundle
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.openTab
import kotlinx.android.synthetic.main.activity_maintenance.*
import org.greenrobot.eventbus.Subscribe

internal class MaintenanceActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maintenance)

        button_read_more?.click {
            openTab("https://mozocoin.io")
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe
    fun onStopMaintenance(event: MessageEvent.StopMaintenanceMode) {
        finish()
    }
}