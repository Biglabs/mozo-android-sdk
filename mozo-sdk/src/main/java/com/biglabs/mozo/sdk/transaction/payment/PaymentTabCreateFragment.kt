package com.biglabs.mozo.sdk.transaction.payment

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.ViewModels
import com.biglabs.mozo.sdk.utils.DecimalDigitsInputFilter
import com.biglabs.mozo.sdk.utils.click
import com.biglabs.mozo.sdk.utils.onTextChanged
import kotlinx.android.synthetic.main.fragment_payment_create.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*

class PaymentTabCreateFragment : Fragment() {

    private var mListener: PaymentRequestInteractionListener? = null
    private var currentRate = BigDecimal.ZERO

    override fun onAttach(context: Context?) {
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

        MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.observe(this, balanceAndRateObserver)

        button_submit.click {
            mListener?.onCreateRequestClicked(output_amount.text.toString())
        }
    }

    private fun initUI() {
        output_amount.onTextChanged {
            it?.toString()?.run {
                if (this.isEmpty()) {
                    output_amount_rate.text = ""
                    updateSubmitButton()
                    return@run
                }
                if (this.startsWith(".")) {
                    output_amount.setText(String.format(Locale.US, "0%s", this))
                    output_amount.setSelection(this.length + 1)
                    return@run
                }
                GlobalScope.launch(Dispatchers.Main) {
                    val amount = BigDecimal(this@run)
                    output_amount_rate.text = MozoSDK.getInstance().profileViewModel
                            .formatCurrencyDisplay(amount.multiply(currentRate))
                }
                updateSubmitButton()
            }
        }
    }

    private fun updateSubmitButton() {
        button_submit.isEnabled = output_amount.length() > 0 && output_amount.text.toString().toBigDecimal() > BigDecimal.ZERO
    }

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> {
        it?.run {
            currentRate = rate
            output_amount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(12, decimal))
        }
    }

    companion object {

        fun getInstance() = PaymentTabCreateFragment()
    }
}