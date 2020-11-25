package io.mozocoin.sdk.transaction.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.ErrorCode
import io.mozocoin.sdk.common.model.PaymentRequest
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.databinding.ActivityPaymentRequestBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.replace

internal class PaymentRequestActivity : BaseActivity(), PaymentRequestInteractionListener {

    private lateinit var binding: ActivityPaymentRequestBinding
    private var isSendCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replace(R.id.payment_request_content_frame, PaymentRequestTabsFragment.getInstance())
    }

    override fun onBackPressed() {
        if (isSendCompleted) finish()
        else {
            super.onBackPressed()
            binding.paymentRequestToolbar.showBackButton(false)
        }
    }

    override fun onCreateRequestClicked(amount: String) {
        binding.paymentRequestToolbar.showBackButton(true)
        replace(
                R.id.payment_request_content_frame,
                PaymentRequestSendFragment.getInstance(amount),
                "send_step"
        )
    }

    override fun onSendRequestClicked(amount: String, toAddress: String, request: PaymentRequest) {
        MozoAPIsService.getInstance().sendPaymentRequest(this, toAddress, request, { data, errorCode ->
            when (errorCode) {
                ErrorCode.ERROR_WALLET_ADDRESS_NOT_EXIST.key -> {
                    MessageDialog.show(this, R.string.error_wallet_not_found)
                    return@sendPaymentRequest
                }
            }
            data?.let {
                isSendCompleted = true
                binding.paymentRequestToolbar.showBackButton(false)
                replace(R.id.payment_request_content_frame, PaymentRequestSentFragment.getInstance(amount, toAddress))
            }
        }, {
            onSendRequestClicked(amount, toAddress, request)
        })
    }

    companion object {
        fun start(context: Context) {
            Intent(context, PaymentRequestActivity::class.java).apply {
                context.startActivity(this)
            }
        }
    }
}