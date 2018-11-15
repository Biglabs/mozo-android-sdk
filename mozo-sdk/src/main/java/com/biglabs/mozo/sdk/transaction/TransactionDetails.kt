package com.biglabs.mozo.sdk.transaction

import androidx.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.contact.AddressAddActivity
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.utils.click
import com.biglabs.mozo.sdk.utils.displayString
import com.biglabs.mozo.sdk.utils.gone
import com.biglabs.mozo.sdk.utils.visible
import kotlinx.android.synthetic.main.view_transaction_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

internal class TransactionDetails : BaseActivity() {

    private var mHistory: Models.TransactionHistory? = null
    private val dateFormat = SimpleDateFormat(Constant.HISTORY_TIME_FORMAT, Locale.getDefault())
    private var findContactJob: Job? = null
    private var targetAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mHistory = intent?.getParcelableExtra(KEY_DATA)
        if (mHistory == null) {
            finishAndRemoveTask()
            return
        }

        setContentView(R.layout.view_transaction_details)

        MozoSDK.getInstance().profileViewModel.run {
            exchangeRateLiveData.observeForever(exchangeRateObserver)
            profileLiveData.observeForever(profileObserver)
        }
        MozoSDK.getInstance().contactViewModel.run {
            contactsLiveData.observeForever(contactObserver)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        findContactJob?.cancel()
        findContactJob = null
        MozoSDK.getInstance().profileViewModel.run {
            exchangeRateLiveData.removeObserver(exchangeRateObserver)
            profileLiveData.removeObserver(profileObserver)
        }
        MozoSDK.getInstance().contactViewModel.run {
            contactsLiveData.removeObserver(contactObserver)
        }
    }

    private val exchangeRateObserver = Observer<Models.ExchangeRate?> {
        if (mHistory != null && it != null) {
            text_detail_amount_rate_side.text = String.format(Locale.US, "(₩%s)", mHistory!!.amountInDecimal().multiply(it.rate.toBigDecimal()).displayString())
        }
    }

    private val profileObserver = Observer<Models.Profile?> {
        it?.run {
            bindData(walletInfo?.offchainAddress ?: "")
        }
    }

    private val contactObserver = Observer<List<Models.Contact>> {
        it?.run {
            displayContact()
        }
    }

    private fun bindData(myAddress: String) {
        mHistory?.apply {
            val sentType = type(myAddress)
            if (sentType) {
                image_tx_type.setBackgroundResource(R.drawable.mozo_bg_icon_send)
                text_detail_status.setText(R.string.mozo_view_text_tx_sent)

                text_detail_receiver_label.setText(R.string.mozo_transfer_receiver_address)
                text_detail_receiver_address.text = addressTo

            } else {
                image_tx_type.setBackgroundResource(R.drawable.mozo_bg_icon_received)
                image_tx_type.rotation = 180f
                text_detail_status.setText(R.string.mozo_view_text_tx_received)

                text_detail_receiver_label.setText(R.string.mozo_view_text_from)
                text_detail_receiver_address.text = addressFrom
            }

            text_detail_time.text = dateFormat.format(Date(time * 1000L))
            text_detail_amount_value.text = amountDisplay()

            targetAddress = if (sentType) addressTo else addressFrom
            displayContact()
        }
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

        fun start(context: Context, history: Models.TransactionHistory) {
            Intent(context, TransactionDetails::class.java).apply {
                putExtra(KEY_DATA, history)
                context.startActivity(this)
            }
        }
    }
}