package com.biglabs.mozo.sdk.pay_request

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import androidx.lifecycle.Observer
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.ViewModels
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.utils.DecimalDigitsInputFilter
import com.biglabs.mozo.sdk.utils.displayString
import com.biglabs.mozo.sdk.utils.onTextChanged
import kotlinx.android.synthetic.main.activity_payment_request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*

internal class PaymentRequestActivity : BaseActivity() {

    private var currentRate = BigDecimal.ZERO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_request)
        initUI()

        MozoSDK.getInstance().profileViewModel.run {
            balanceAndRateLiveData.observeForever(balanceAndRateObserver)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MozoSDK.getInstance().profileViewModel.run {
            balanceAndRateLiveData.removeObserver(balanceAndRateObserver)
        }
    }

    private fun initUI() {
        output_amount.onTextChanged {
            updateSubmitButton()

            it?.toString()?.run {
                if (this.isEmpty()) {
                    output_amount_rate.text = ""
                    return@run
                }
                if (this.startsWith(".")) {
                    output_amount.setText(String.format(Locale.US, "0%s", this))
                    output_amount.setSelection(this.length + 1)
                    return@run
                }
                GlobalScope.launch(Dispatchers.Main) {
                    val amount = BigDecimal(this@run)
                    val rate = String.format(Locale.US, "₩%s", amount.multiply(currentRate).displayString())
                    output_amount_rate.text = rate
                }
            }
        }
    }

    private fun updateSubmitButton() {
        button_submit.isEnabled = output_amount.length() > 0
    }

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> {
        it?.run {
            currentRate = rate
            output_amount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(12, decimal))
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