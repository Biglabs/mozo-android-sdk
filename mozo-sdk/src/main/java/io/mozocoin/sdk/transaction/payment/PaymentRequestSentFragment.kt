package io.mozocoin.sdk.transaction.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.displayString
import io.mozocoin.sdk.utils.safe
import kotlinx.android.synthetic.main.fragment_payment_sent.*
import java.math.BigDecimal

class PaymentRequestSentFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_payment_sent, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val address = arguments?.getString(KEY_ADDRESS)
        payment_request_address.text = MozoSDK.getInstance()
                .contactViewModel
                .findByAddress(address)?.name ?: address

        val amount = arguments?.getString(KEY_AMOUNT)?.toBigDecimal().safe()
        payment_request_amount.text = amount.displayString()

        MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.observe(this, Observer {
            payment_request_rate.text = MozoSDK.getInstance().profileViewModel
                    .formatCurrencyDisplay(
                            amount.multiply(it.rate),
                            true
                    )
        })
    }

    companion object {
        private const val KEY_AMOUNT = "key_amount"
        private const val KEY_ADDRESS = "key_address"

        fun getInstance(amount: String, toAddress: String) = PaymentRequestSentFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_AMOUNT, amount)
                putString(KEY_ADDRESS, toAddress)
            }
        }
    }
}