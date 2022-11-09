package io.mozocoin.sdk.ui

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.activity.OnBackPressedCallback
import androidx.core.text.HtmlCompat
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.MessageEvent
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.databinding.ActivityMaintenanceBinding
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.openTab
import io.mozocoin.sdk.utils.safe
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.TimeUnit
import kotlin.random.Random

internal class MaintenanceActivity : BaseActivity() {

    private lateinit var binding: ActivityMaintenanceBinding
    private var tipsQuestion: Array<String>? = null
    private var tipsAnswer: Array<String>? = null
    private var tipsUrls: Array<String>? = null

    private var referenceUrl: String? = null

    private var mSystemStatusCheckJob: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaintenanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonReadMore.click {
            openTab(referenceUrl ?: return@click)
        }
        binding.maintenanceTipsContent.movementMethod = LinkMovementMethod.getInstance()

        randomTips()

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                /**
                 * No need to do anything
                 * Not allowed to back to previous screen during Maintenance mode
                 */
            }
        })
    }

    override fun onResume() {
        super.onResume()
        intervalStatusCheck()
    }

    override fun onPause() {
        super.onPause()
        mSystemStatusCheckJob?.dispose()
        mSystemStatusCheckJob = null
    }

    private fun randomTips() {
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

        binding.maintenanceTipsTitle.text = tipsQuestion?.getOrNull(index)
        binding.maintenanceTipsContent.text = HtmlCompat.fromHtml(
            tipsAnswer?.getOrNull(index).safe(),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        referenceUrl = "https://${Support.domainHomePage()}/${tipsUrls?.getOrNull(index) ?: ""}"
    }

    private fun intervalStatusCheck() {
        mSystemStatusCheckJob?.dispose()
        mSystemStatusCheckJob = Observable.interval(
            1,
            INTERVAL_DELAY_IN_SECOND,
            TimeUnit.SECONDS
        )
            .subscribeOn(Schedulers.single())
            .doOnEach {
                MozoAPIsService.getInstance()
                    .checkSystemStatus(this@MaintenanceActivity) { data, _ ->
                        if (FLAG_STATUS_GOOD.equals(data?.status, ignoreCase = true)) {
                            finish()
                            return@checkSystemStatus
                        }
                    }
            }
            .subscribe()
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe
    fun onStopMaintenance(event: MessageEvent.StopMaintenanceMode) {
        finish()
    }

    companion object {
        private const val INTERVAL_DELAY_IN_SECOND = 15L
        private const val FLAG_STATUS_GOOD = "HEALTHY"
    }
}