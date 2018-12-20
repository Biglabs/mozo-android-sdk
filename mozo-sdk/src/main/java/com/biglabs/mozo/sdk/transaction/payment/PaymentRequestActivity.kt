package com.biglabs.mozo.sdk.transaction.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.utils.replace
import kotlinx.android.synthetic.main.activity_payment_request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class PaymentRequestActivity : BaseActivity(), PaymentRequestInteractionListener {

    private var isSendCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_request)

        replace(R.id.payment_request_content_frame, PaymentRequestTabsFragment.getInstance())
    }

    override fun onBackPressed() {
        if (isSendCompleted) finish()
        else {
            super.onBackPressed()
            payment_request_toolbar?.showBackButton(false)
        }
    }

    override fun onCreateRequestClicked(amount: String) {
        payment_request_toolbar.showBackButton(true)
        replace(
                R.id.payment_request_content_frame,
                PaymentRequestSendFragment.getInstance(amount),
                "send_step"
        )
    }

    override fun onSendRequestClicked(amount: String, toAddress: String, request: Models.PaymentRequest) {
        GlobalScope.launch {
            val response = MozoService
                    .getInstance(this@PaymentRequestActivity)
                    .sendPaymentRequest(toAddress, request) {
                        onSendRequestClicked(amount, toAddress, request)
                    }.await()

            launch(Dispatchers.Main) {
                response?.let {
                    isSendCompleted = true
                    payment_request_toolbar.showBackButton(false)
                    replace(R.id.payment_request_content_frame, PaymentRequestSentFragment.getInstance(amount, toAddress))
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            Intent(context, PaymentRequestActivity::class.java).apply {
                context.startActivity(this)
            }
        }
    }
}