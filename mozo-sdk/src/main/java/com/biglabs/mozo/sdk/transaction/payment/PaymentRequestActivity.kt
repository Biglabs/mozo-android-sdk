package com.biglabs.mozo.sdk.transaction.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.ErrorCode
import com.biglabs.mozo.sdk.common.model.PaymentRequest
import com.biglabs.mozo.sdk.common.service.MozoAPIsService
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.ui.dialog.MessageDialog
import com.biglabs.mozo.sdk.utils.replace
import kotlinx.android.synthetic.main.activity_payment_request.*

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

    override fun onSendRequestClicked(amount: String, toAddress: String, request: PaymentRequest) {
        MozoAPIsService.getInstance().sendPaymentRequest(this, toAddress, request) { data, errorCode ->
            when (errorCode) {
                ErrorCode.ERROR_WALLET_ADDRESS_NOT_EXIST.key -> {
                    MessageDialog.show(this, R.string.error_wallet_not_found)
                    return@sendPaymentRequest
                }
            }
            data?.let {
                isSendCompleted = true
                payment_request_toolbar.showBackButton(false)
                replace(R.id.payment_request_content_frame, PaymentRequestSentFragment.getInstance(amount, toAddress))
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