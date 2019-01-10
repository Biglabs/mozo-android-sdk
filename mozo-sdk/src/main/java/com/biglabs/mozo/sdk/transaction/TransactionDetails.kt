package com.biglabs.mozo.sdk.transaction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.MozoWallet
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.model.Contact
import com.biglabs.mozo.sdk.common.model.PaymentRequest
import com.biglabs.mozo.sdk.common.model.TransactionHistory
import com.biglabs.mozo.sdk.contact.AddressAddActivity
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.ui.dialog.MessageDialog
import com.biglabs.mozo.sdk.utils.*
import kotlinx.android.synthetic.main.view_transaction_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*

internal class TransactionDetails : BaseActivity() {

    private var mHistory: TransactionHistory? = null
    private var mPaymentRequest: PaymentRequest? = null

    private var findContactJob: Job? = null
    private var targetAddress: String? = null
    private var currentBalance = BigDecimal.ZERO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mHistory = intent?.getParcelableExtra(KEY_DATA)
        mPaymentRequest = intent?.getParcelableExtra(KEY_DATA_PAYMENT)

        if (mHistory == null && mPaymentRequest == null) {
            finishAndRemoveTask()
            return
        }

        setContentView(R.layout.view_transaction_details)

        var amount = BigDecimal.ZERO
        when {
            mHistory != null -> {
                amount = mHistory!!.amountInDecimal()
                setTitle(R.string.mozo_transaction_detail_title)
            }
            mPaymentRequest != null -> {
                mPaymentRequest!!.content ?: return
                val data = Support.parsePaymentRequest(mPaymentRequest!!.content!!)
                targetAddress = data.firstOrNull()
                data.lastOrNull()?.let {
                    amount = it.toBigDecimal()
                }

                setTitle(R.string.mozo_payment_request_title)
                button_pay.visible()
                button_pay.click {
                    if (amount > currentBalance) {
                        MessageDialog.show(this, R.string.mozo_dialog_error_not_enough_msg)
                    } else
                        TransactionFormActivity.start(this, targetAddress, data.lastOrNull())
                }
            }
        }

        bindData(MozoWallet.getInstance().getAddress() ?: "")

        MozoSDK.getInstance().contactViewModel.contactsLiveData.observe(this, Observer<List<Contact>> {
            it?.run {
                displayContact()
            }
        })
        MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.observe(this, Observer {
            currentBalance = it.balanceInDecimal
            it?.rate?.run {
                text_detail_amount_rate_side.text = String.format(Locale.US, "(₩%s)", amount.multiply(this).displayString())
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        findContactJob?.cancel()
        findContactJob = null
    }

    private fun bindData(myAddress: String) {
        var sendType = true
        var detailTime = 0L
        var amountDisplay = ""
        when {
            mHistory != null -> {
                sendType = mHistory!!.type(myAddress)
                text_detail_status.setText(if (sendType) R.string.mozo_view_text_tx_sent else R.string.mozo_view_text_tx_received)
                targetAddress = if (sendType) mHistory!!.addressTo else mHistory!!.addressFrom

                detailTime = mHistory!!.time * 1000L
                amountDisplay = mHistory!!.amountDisplay()
            }
            mPaymentRequest != null -> {
                mPaymentRequest!!.content ?: return
                val data = Support.parsePaymentRequest(mPaymentRequest!!.content!!)
                targetAddress = data.firstOrNull()

                text_detail_status.setText(R.string.mozo_button_transfer)
                detailTime = mPaymentRequest!!.timeInSec * 1000
                data.lastOrNull()?.let {
                    amountDisplay = it.toBigDecimal().displayString()
                }
            }
        }
        if (sendType) {
            image_tx_type.setBackgroundResource(R.drawable.mozo_bg_icon_send)
            text_detail_receiver_label.setText(R.string.mozo_transfer_receiver_address)

        } else {
            image_tx_type.setBackgroundResource(R.drawable.mozo_bg_icon_received)
            image_tx_type.rotation = 180f
            text_detail_receiver_label.setText(R.string.mozo_view_text_from)
        }

        text_detail_receiver_address.text = targetAddress

        text_detail_time.text = if (detailTime > 0) Support.getDisplayDate(detailTime, Constant.HISTORY_TIME_FORMAT) else ""
        text_detail_amount_value.text = amountDisplay

        displayContact()
    }

    private fun displayContact() {
        findContactJob?.cancel()
        findContactJob = GlobalScope.launch {
            val contact = MozoSDK.getInstance().contactViewModel.findByAddress(targetAddress)
            launch(Dispatchers.Main) {
                if (contact != null) {
                    text_detail_receiver_user_name.text = contact.name
                    visible(arrayOf(
                            text_detail_to_label,
                            text_detail_receiver_icon,
                            text_detail_receiver_user_name
                    ))
                    gone(arrayOf(
                            button_save_address,
                            button_save_address_top_line
                    ))
                } else {
                    visible(arrayOf(
                            button_save_address,
                            button_save_address_top_line
                    ))
                    gone(arrayOf(
                            text_detail_to_label,
                            text_detail_receiver_icon,
                            text_detail_receiver_user_name
                    ))
                    button_save_address.click { AddressAddActivity.start(this@TransactionDetails, targetAddress) }
                }
            }
            findContactJob = null
        }
    }

    companion object {
        private const val KEY_DATA = "KEY_DATA"
        private const val KEY_DATA_PAYMENT = "KEY_DATA_PAYMENT"

        fun start(context: Context, history: TransactionHistory) {
            Intent(context, TransactionDetails::class.java).apply {
                putExtra(KEY_DATA, history)
                context.startActivity(this)
            }
        }

        fun start(context: Context, paymentRequest: PaymentRequest) {
            Intent(context, TransactionDetails::class.java).apply {
                putExtra(KEY_DATA_PAYMENT, paymentRequest)
                context.startActivity(this)
            }
        }
    }
}