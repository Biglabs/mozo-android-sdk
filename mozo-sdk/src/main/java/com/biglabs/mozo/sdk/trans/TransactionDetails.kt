package com.biglabs.mozo.sdk.trans

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Constant
import com.biglabs.mozo.sdk.core.Models
import com.biglabs.mozo.sdk.services.AddressBookService
import com.biglabs.mozo.sdk.utils.gone
import com.biglabs.mozo.sdk.utils.visible
import kotlinx.android.synthetic.main.view_transaction_details.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionDetails : AppCompatActivity() {

    private val dateFormat = SimpleDateFormat(Constant.HISTORY_TIME_FORMAT, Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent == null) return

        setContentView(R.layout.view_transaction_details)

        val history = intent.getParcelableExtra<Models.TransactionHistory>(KEY_DATA)
        bindData(history)
    }

    private fun bindData(history: Models.TransactionHistory) {
        val sentType = history.type("")
        if (sentType) {
            text_detail_status.setText(R.string.mozo_view_text_tx_sent)

            visible(arrayOf(
                    text_detail_balance_label,
                    icon_balance,
                    text_detail_balance_value,
                    text_detail_balance_rate_side,
                    text_detail_balance_underline
            ))

            text_detail_receiver_label.setText(R.string.mozo_transfer_receiver_address)
            text_detail_receiver_address.text = history.addressTo

            launch {
                val balance = MozoTrans.getInstance().getBalance().await() ?: "0"
                launch(UI) {
                    text_detail_balance_value.text = balance
                }
            }
        } else {
            text_detail_status.setText(R.string.mozo_view_text_tx_received)

            gone(arrayOf(
                    text_detail_balance_label,
                    icon_balance,
                    text_detail_balance_value,
                    text_detail_balance_rate_side,
                    text_detail_balance_underline
            ))

            text_detail_receiver_label.setText(R.string.mozo_view_text_from)
            text_detail_receiver_address.text = history.addressFrom
        }

        text_detail_time.text = dateFormat.format(Date(history.time * 1000L))

        val contact = AddressBookService.getInstance().findByAddress(history.addressTo)
        if (contact != null) {
            text_detail_receiver_user_name.text = contact.name
            visible(arrayOf(
                    text_detail_receiver_icon,
                    text_detail_receiver_user_name
            ))
        } else {
            gone(arrayOf(
                    text_detail_receiver_icon,
                    text_detail_receiver_user_name
            ))
        }

        text_detail_amount_value.text = history.amountDisplay()
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