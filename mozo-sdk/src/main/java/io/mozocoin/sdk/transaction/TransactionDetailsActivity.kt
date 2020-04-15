package io.mozocoin.sdk.transaction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.common.model.PaymentRequest
import io.mozocoin.sdk.common.model.TransactionHistory
import io.mozocoin.sdk.contact.AddressAddActivity
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.*
import kotlinx.android.synthetic.main.view_transaction_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal

internal class TransactionDetailsActivity : BaseActivity() {

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
            mHistory != null        -> {
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
                        TransactionFormActivity.start(this, targetAddress, amount.toString())
                }
            }
        }

        bindData(MozoWallet.getInstance().getAddress() ?: "")

        MozoSDK.getInstance().contactViewModel.usersLiveData.observe(this, Observer<List<Contact>> {
            it?.run {
                displayContact()
            }
        })
        MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.observe(this, Observer {
            currentBalance = it.balanceNonDecimal
            it?.rate?.run {
                text_detail_amount_rate_side.text = MozoSDK.getInstance().profileViewModel
                    .formatCurrencyDisplay(amount.multiply(this), true)
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
            mHistory != null        -> {
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

                text_detail_status.setText(R.string.mozo_button_send_mozo)
                data.lastOrNull()?.let {
                    amountDisplay = it.toBigDecimal().displayString()
                }
            }
        }
        if (sendType) {
            text_detail_receiver?.setText(R.string.mozo_view_text_to)
            image_tx_type?.setBackgroundResource(R.drawable.mozo_bg_icon_send)

        } else {
            text_detail_receiver?.setText(R.string.mozo_view_text_from)
            image_tx_type?.setBackgroundResource(R.drawable.mozo_bg_icon_received)
            image_tx_type?.rotation = 180f
        }

        text_detail_receiver_wallet_address?.text = targetAddress

        if (detailTime > 0) {
            text_detail_time.text =
                Support.getDisplayDate(this, detailTime, string(R.string.mozo_format_date_time))
            text_detail_time.isVisible = true
        } else text_detail_time.isVisible = false

        text_detail_amount_value.text = amountDisplay

        displayContact()
    }

    private fun displayContact() {
        findContactJob?.cancel()
        findContactJob = GlobalScope.launch {
            val contact = MozoSDK.getInstance().contactViewModel.findByAddress(targetAddress)
            launch(Dispatchers.Main) {
                val isContact = contact != null
                val isStore = contact?.isStore == true

                button_save_address?.isGone = isContact && !contact?.name.isNullOrEmpty()
                button_save_address_top_line?.isGone = isContact
                image_detail_receiver?.isVisible = isContact

                text_detail_receiver_name?.isVisible = isContact
                text_detail_receiver_phone?.isVisible =
                    isContact && !isStore && !contact?.phoneNo.isNullOrEmpty()
                text_detail_store_address?.isVisible = isContact && isStore

                if (contact != null) {
                    image_detail_receiver?.setImageResource(
                        if (isStore) R.drawable.ic_store
                        else R.drawable.ic_receiver
                    )
                    text_detail_receiver_name?.text = contact.name
                    text_detail_store_address?.text = contact.physicalAddress
                    text_detail_receiver_phone?.text = contact.phoneNo

                } else button_save_address?.click {
                    AddressAddActivity.start(this@TransactionDetailsActivity, targetAddress)
                }
            }
            findContactJob = null
        }
    }

    companion object {
        private const val KEY_DATA = "KEY_DATA"
        private const val KEY_DATA_PAYMENT = "KEY_DATA_PAYMENT"

        fun start(context: Context, history: TransactionHistory) {
            Intent(context, TransactionDetailsActivity::class.java).apply {
                putExtra(KEY_DATA, history)
                context.startActivity(this)
            }
        }

        fun start(context: Context, paymentRequest: PaymentRequest) {
            Intent(context, TransactionDetailsActivity::class.java).apply {
                putExtra(KEY_DATA_PAYMENT, paymentRequest)
                context.startActivity(this)
            }
        }
    }
}