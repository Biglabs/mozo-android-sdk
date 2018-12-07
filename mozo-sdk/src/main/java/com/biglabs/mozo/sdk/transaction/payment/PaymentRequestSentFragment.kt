package com.biglabs.mozo.sdk.transaction.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Models
import kotlinx.android.synthetic.main.fragment_payment_sent.*

class PaymentRequestSentFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_payment_sent, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getParcelable<Models.PaymentRequest>(KEY_DATA)?.let {
            //            payment_request_amount
//            payment_request_rate
            payment_request_address.text = it.toAddress
        }
    }

    companion object {
        private const val KEY_AMOUNT = "key_amount"
        private const val KEY_DATA = "key_data"

        fun getInstance(amount: String, data: Models.PaymentRequest) = PaymentRequestSentFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_AMOUNT, amount)
                putParcelable(KEY_DATA, data)
            }
        }
    }
}