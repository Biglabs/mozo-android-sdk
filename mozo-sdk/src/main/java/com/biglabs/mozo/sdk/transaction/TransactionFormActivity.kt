package com.biglabs.mozo.sdk.transaction

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.text.set
import androidx.lifecycle.Observer
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.MozoTx
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.ViewModels
import com.biglabs.mozo.sdk.common.model.Contact
import com.biglabs.mozo.sdk.common.model.TransactionHistory
import com.biglabs.mozo.sdk.common.model.TransactionHistory.CREATOR.MY_ADDRESS
import com.biglabs.mozo.sdk.common.model.TransactionResponse
import com.biglabs.mozo.sdk.contact.AddressAddActivity
import com.biglabs.mozo.sdk.contact.AddressBookActivity
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.ui.SecurityActivity
import com.biglabs.mozo.sdk.utils.*
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.view_transaction_form.*
import kotlinx.android.synthetic.main.view_transaction_sent.*
import kotlinx.coroutines.*
import org.web3j.crypto.WalletUtils
import java.math.BigDecimal
import java.util.*

@Suppress("unused")
internal class TransactionFormActivity : BaseActivity() {

    private var currentBalance = BigDecimal.ZERO
    private var currentRate = BigDecimal.ZERO
    private var selectedContact: Contact? = null
    private val history = TransactionHistory("", 0L, "", 0.0, BigDecimal.ZERO, MY_ADDRESS, "", "", "", "", 2, 0L, "")
    private var updateTxStatusJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_transaction_form)

        initUI()
        showInputUI()

        MozoSDK.getInstance().profileViewModel.run {
            balanceAndRateLiveData.observe(this@TransactionFormActivity, balanceAndRateObserver)
            fetchBalance(this@TransactionFormActivity)
        }

        val address = intent?.getStringExtra(KEY_DATA_ADDRESS)
        val amount = intent?.getStringExtra(KEY_DATA_AMOUNT)
        if (address != null && amount != null) {
            selectedContact = MozoSDK.getInstance().contactViewModel.findByAddress(address)
            output_receiver_address.setText(address)
            output_amount.setText(amount)
            showConfirmationUI()
            button_submit.performClick()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        selectedContact = null
        updateTxStatusJob?.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK) return
        when {
            requestCode == KEY_PICK_ADDRESS -> {
                data?.run {
                    selectedContact = getParcelableExtra(AddressBookActivity.KEY_SELECTED_ADDRESS)
                    showContactInfoUI()
                }
            }
            requestCode == KEY_VERIFY_PIN -> {
                data?.run {
                    sendTx(getStringExtra(SecurityActivity.KEY_DATA))
                }
            }
            data != null -> {
                IntentIntegrator
                        .parseActivityResult(requestCode, resultCode, data)
                        .contents?.let {
                    selectedContact = MozoSDK.getInstance().contactViewModel.findByAddress(it)

                    showInputUI()
                    if (selectedContact == null) {
                        output_receiver_address.setText(it)
                        updateSubmitButton()
                    } else
                        showContactInfoUI()
                }
            }
        }
    }

    private fun sendTx(pin: String?) {
        if (pin == null) return
        val address = selectedContact?.walletAddress ?: output_receiver_address.text.toString()
        val amount = output_amount.text.toString()

        showLoading()
        MozoTx.getInstance().createTransaction(this, address, amount, pin) {
            hideLoading()
            history.addressTo = address
            history.amount = MozoTx.getInstance().amountWithDecimal(amount)
            history.time = Calendar.getInstance().timeInMillis / 1000L
            showResultUI(it)
        }
    }

    override fun onBackPressed() {
        if (output_receiver_address.isEnabled) {
            super.onBackPressed()
        } else {
            showInputUI()
            showContactInfoUI()
        }
    }

    private fun initUI() {
        output_receiver_address.onTextChanged {
            hideErrorAddressUI()
            updateSubmitButton()
        }
        output_receiver_address.setOnFocusChangeListener { _, hasFocus ->
            output_receiver_address_label.isSelected = hasFocus
            output_receiver_address_underline.isSelected = hasFocus
        }
        output_amount.onTextChanged {
            hideErrorAmountUI()
            updateSubmitButton()

            it?.toString()?.run {
                if (this.isEmpty()) {
                    output_amount_rate.text = ""
                    text_preview_rate.text = ""
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
                    text_preview_rate.text = String.format(Locale.US, "(%s)", rate)
                }
            }
        }
        output_amount.setOnFocusChangeListener { _, hasFocus ->
            output_amount_label.isSelected = hasFocus
            output_amount_underline.isSelected = hasFocus
        }

        transfer_toolbar.onBackPress = { onBackPressed() }
        button_address_book.click { AddressBookActivity.startForResult(this, KEY_PICK_ADDRESS) }
        button_scan_qr.click {
            Support.scanQRCode(this)
        }
        button_submit.click {
            if (output_receiver_address.isEnabled) {
                if (validateInput()) showConfirmationUI()
            } else {
                SecurityActivity.start(this, SecurityActivity.KEY_VERIFY_PIN_FOR_SEND, KEY_VERIFY_PIN)
            }
        }
    }

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> { bar ->
        bar?.run {
            currentBalance = balanceInDecimal
            currentRate = rate
            text_spendable.text = SpannableString(getString(R.string.mozo_transfer_spendable, currentBalance.displayString())).apply {
                set(indexOfFirst { it.isDigit() }..length, ForegroundColorSpan(color(R.color.mozo_color_primary)))
            }
            output_amount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(12, decimal))
        }
    }

    private fun showInputUI() {
        output_receiver_address.isEnabled = true
        output_amount.isEnabled = true
        visible(arrayOf(
                output_receiver_address,
                output_receiver_address_underline,
                button_address_book,
                button_scan_qr,
                output_amount,
                output_amount_rate,
                output_amount_underline
        ))
        gone(arrayOf(
                output_receiver_address_user,
                send_state_container,
                confirmation_state_separator,
                output_amount_preview_container
        ))

        transfer_toolbar.showBackButton(false)
        button_submit.setText(R.string.mozo_button_continue)

        if (output_amount_error_msg.visibility != View.VISIBLE) {
            text_spendable.visible()
        }
    }

    private fun showContactInfoUI() {
        hideErrorAddressUI()
        updateSubmitButton()
        selectedContact?.run {
            output_receiver_address.visibility = View.INVISIBLE
            output_receiver_address_underline.visibility = View.INVISIBLE
            button_scan_qr.visibility = View.INVISIBLE

            output_receiver_address_user.visible()
            text_receiver_user_name.text = name
            text_receiver_user_address.text = walletAddress

            button_clear.apply {
                visible()
                click {
                    selectedContact = null
                    showInputUI()
                    updateSubmitButton()
                }
            }
        }
    }

    private fun showConfirmationUI() {
        output_receiver_address.isEnabled = false
        output_amount.isEnabled = false
        gone(arrayOf(
                output_receiver_address_underline,
                button_address_book,
                button_scan_qr,
                output_amount,
                output_amount_rate,
                output_amount_underline,
                text_spendable,
                button_clear
        ))
        text_preview_amount.text = output_amount.text
        visible(arrayOf(
                send_state_container,
                confirmation_state_separator,
                output_amount_preview_container
        ))

        transfer_toolbar.showBackButton(true)
        button_submit.setText(R.string.mozo_button_send)
    }

    private fun showResultUI(txResponse: TransactionResponse?) = GlobalScope.launch(Dispatchers.Main) {
        if (txResponse != null) {
            setContentView(R.layout.view_transaction_sent)

            text_preview_amount_sent.text = history.amountDisplay()
            text_preview_address_sent.text = history.addressTo
            text_preview_rate_sent.text = String.format(Locale.US, "(₩%s)", history.amountInDecimal().multiply(currentRate).displayString())

            button_save_address?.apply {
                if (selectedContact != null) gone() else visible()
                click {
                    AddressAddActivity.start(this@TransactionFormActivity, history.addressTo)
                }
            }

            history.txHash = txResponse.tx.hash ?: ""

            button_transaction_detail?.apply {
                gone()
                click {
                    TransactionDetails.start(this@TransactionFormActivity, history)
                }
            }

            updateTxStatus()
        }
    }

    private fun updateTxStatus() {
        MozoTx.getInstance().getTransactionStatus(this, history.txHash ?: return) {
            when {
                it.isSuccess() -> {
                    transfer_info_container.visible()
                    transfer_status_container.gone()
                    button_transaction_detail.visible()
                }
                it.isFailed() -> {
                    text_tx_status_icon.setImageResource(R.drawable.ic_error)
                    text_tx_status_label.setText(R.string.mozo_view_text_tx_failed)
                }
            }

            updateTxStatusJob = if (it.isSuccess() || it.isFailed()) {
                text_tx_status_loading.gone()
                text_tx_status_icon.visible()
                null
            } else /* PENDING */ {
                updateTxStatusJob?.cancel()
                GlobalScope.launch {
                    delay(1500)
                    updateTxStatus()
                }
            }
        }
    }

    private fun updateSubmitButton() {
        button_submit.isEnabled = (selectedContact != null || output_receiver_address.length() > 0) && output_amount.length() > 0
    }

    private fun showLoading() = GlobalScope.launch(Dispatchers.Main) {
        loading_container.visible()
    }

    private fun hideLoading() = GlobalScope.launch(Dispatchers.Main) {
        loading_container.gone()
    }

    private fun showErrorAddressUI() {
        val errorColor = color(R.color.mozo_color_error)
        output_receiver_address_label.setTextColor(errorColor)
        output_receiver_address_underline.setBackgroundColor(errorColor)
        output_receiver_address_error_msg.visible()
    }

    private fun hideErrorAddressUI() {
        output_receiver_address_label.setTextColor(ContextCompat.getColorStateList(this, R.color.mozo_color_input_focus))
        output_receiver_address_underline.setBackgroundResource(R.drawable.mozo_color_line_focus)
        output_receiver_address_error_msg.gone()
    }

    private fun showErrorAmountUI(@StringRes errorId: Int? = null) {
        val errorColor = color(R.color.mozo_color_error)
        output_amount_label.setTextColor(errorColor)
        output_amount_underline.setBackgroundColor(errorColor)
        output_amount_error_msg.setText(errorId ?: R.string.mozo_transfer_amount_error)
        output_amount_error_msg.visible()
        text_spendable.gone()
    }

    private fun hideErrorAmountUI() {
        output_amount_label.setTextColor(ContextCompat.getColorStateList(this, R.color.mozo_color_input_focus))
        output_amount_underline.setBackgroundResource(R.drawable.mozo_color_line_focus)
        output_amount_error_msg.gone()
        text_spendable.visible()
    }

    @SuppressLint("SetTextI18n")
    private fun validateInput(): Boolean {
        var isValidAddress = true
        val address = selectedContact?.walletAddress ?: output_receiver_address.text.toString()
        if (!WalletUtils.isValidAddress(address)) {
            showErrorAddressUI()
            isValidAddress = false
        }

        var isValidAmount = true
        var amount = output_amount.text.toString()
        if (amount.startsWith(".")) {
            amount = "0$amount"
        }
        if (amount.endsWith(".")) {
            amount = "${amount}0"
        }
        output_amount.setText(amount)
        val bigAmount = BigDecimal(amount)
        when {
            bigAmount > currentBalance -> {
                showErrorAmountUI(R.string.mozo_transfer_amount_error_not_enough)
                isValidAmount = false
            }
            bigAmount <= BigDecimal.ZERO -> {
                showErrorAmountUI(R.string.mozo_transfer_amount_error_too_low)
                isValidAmount = false
            }
        }

        return isValidAddress && isValidAmount
    }

    companion object {
        private const val KEY_PICK_ADDRESS = 0x0021
        private const val KEY_VERIFY_PIN = 0x0022
        private const val KEY_DATA_ADDRESS = "key_data_address"
        private const val KEY_DATA_AMOUNT = "key_data_amount"

        fun start(context: Context) {
            Intent(context, TransactionFormActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(this)
            }
        }

        fun start(context: Context, address: String?, amount: String?) {
            Intent(context, TransactionFormActivity::class.java).apply {
                putExtra(KEY_DATA_ADDRESS, address)
                putExtra(KEY_DATA_AMOUNT, amount)
                context.startActivity(this)
            }
        }
    }
}