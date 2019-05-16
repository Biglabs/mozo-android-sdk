package io.mozocoin.sdk.ui

import android.os.Bundle
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.openTab
import kotlinx.android.synthetic.main.activity_maintenance.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import kotlin.random.Random

internal class MaintenanceActivity : BaseActivity() {

    private var tipsQuestion: Array<String>? = null
    private var tipsAnswer: Array<String>? = null
    private var tipsUrls: Array<String>? = null

    private var referenceUrl: String? = null

    private var mSystemStatusCheckJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maintenance)

        button_read_more?.click {
            openTab(referenceUrl ?: return@click)
        }
    }

    override fun onResume() {
        super.onResume()
        tipsQuestion = tipsQuestion ?: resources.getStringArray(
                if (MozoSDK.isRetailerApp) R.array.tips_retailer_question
                else R.array.tips_shopper_question
        )

        tipsAnswer = tipsAnswer ?: resources.getStringArray(
                if (MozoSDK.isRetailerApp) R.array.tips_retailer_answer
                else R.array.tips_shopper_answer
        )

        tipsUrls = tipsUrls ?: resources.getStringArray(
                if (MozoSDK.isRetailerApp) R.array.tips_retailer_url
                else R.array.tips_shopper_url
        )

        val index = Random.nextInt(tipsQuestion?.size ?: 0)

        maintenance_tips_title?.text = tipsQuestion?.getOrNull(index)
        maintenance_tips_content?.text = tipsAnswer?.getOrNull(index)
        referenceUrl = "https://${Support.domainLandingPage()}/${tipsUrls?.getOrNull(index) ?: ""}"

        intervalStatusCheck()
    }

    override fun onPause() {
        super.onPause()
        mSystemStatusCheckJob?.cancel()
        mSystemStatusCheckJob = null
    }

    override fun onBackPressed() {
        /**
         * Not allowed to back to previous screen during Maintenance mode
         * super.onBackPressed()
         */
    }

    private fun intervalStatusCheck() {
        mSystemStatusCheckJob?.cancel()
        mSystemStatusCheckJob = GlobalScope.launch {
            delay(INTERVAL_DELAY)

            MozoAPIsService.getInstance().checkSystemStatus(this@MaintenanceActivity) { data, _ ->
                if (FLAG_STATUS_GOOD.equals(data?.status, ignoreCase = true)) {
                    finish()
                    return@checkSystemStatus
                }

                intervalStatusCheck()
            }
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe
    fun onStopMaintenance(event: MessageEvent.StopMaintenanceMode) {
        finish()
    }

    companion object {
        private const val INTERVAL_DELAY = 15000L

        private const val FLAG_STATUS_GOOD = "HEALTHY"
    }
}