package io.mozocoin.sdk.transaction.payment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.onAmountInputChanged
import kotlinx.android.synthetic.main.fragment_payment_create.*
import java.math.BigDecimal

class PaymentTabCreateFragment : Fragment() {

    private var mListener: PaymentRequestInteractionListener? = null
    private var mInputAmount = BigDecimal.ZERO

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PaymentRequestActivity) {
            mListener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_payment_create, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()

        button_submit.click {
            mListener?.onCreateRequestClicked(mInputAmount.toString())
        }
    }

    private fun initUI() {
        output_amount?.onAmountInputChanged(
                textChanged = {
                    updateSubmitButton()
                    if (it.isNullOrEmpty()) {
                        output_amount_rate?.text = ""
                        return@onAmountInputChanged
                    }

                },
                amountChanged = {
                    mInputAmount = it
                    output_amount_rate?.text = MozoWallet.getInstance().amountInCurrency(mInputAmount)
                }
        )
    }

    private fun updateSubmitButton() {
        button_submit.isEnabled = output_amount.length() > 0 && mInputAmount > BigDecimal.ZERO
    }

    companion object {

        fun getInstance() = PaymentTabCreateFragment()
    }
}