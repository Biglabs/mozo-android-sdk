package com.biglabs.mozo.sdk.transaction

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.text.InputFilter
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.MozoTrans
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.Models.TransactionHistory.CREATOR.MY_ADDRESS
import com.biglabs.mozo.sdk.common.ViewModels
import com.biglabs.mozo.sdk.contact.AddressAddActivity
import com.biglabs.mozo.sdk.contact.AddressBookActivity
import com.biglabs.mozo.sdk.ui.BaseActivity
import com.biglabs.mozo.sdk.ui.SecurityActivity
import com.biglabs.mozo.sdk.utils.*
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.view_transaction_form.*
import kotlinx.android.synthetic.main.view_transaction_sent.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.web3j.crypto.WalletUtils
import java.math.BigDecimal
import java.util.*

@Suppress("unused")
internal class TransactionFormActivity : BaseActivity() {

    private var currentBalance = BigDecimal.ZERO
    private var currentRate = BigDecimal.ZERO
    private var selectedContact: Models.Contact? = null
    private val history = Models.TransactionHistory("", 0L, "", 0.0, BigDecimal.ZERO, MY_ADDRESS, "", "", "", "", 2, 0L, "")
    private var updateTxStatusJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_transaction_form)

        initUI()
        showInputUI()

        MozoSDK.getInstance().profileViewModel.fetchBalance(this)
        MozoSDK.getInstance().contactViewModel.run {
            contactsLiveData.observeForever(contactsObserver)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        selectedContact = null
        updateTxStatusJob?.cancel()
        MozoSDK.getInstance().profileViewModel.run {
            balanceAndRateLiveData.removeObserver(balanceAndRateObserver)
        }
        MozoSDK.getInstance().contactViewModel.run {
            contactsLiveData.removeObserver(contactsObserver)
        }
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
        val address = selectedContact?.soloAddress ?: output_receiver_address.text.toString()
        val amount = output_amount.text.toString()
        launch {
            showLoading()
            val txResponse = MozoTrans.getInstance()
                    .createTransaction(this@TransactionFormActivity, address, amount, pin) {
                        sendTx(pin)
                    }.await()
            history.addressTo = address
            history.amount = MozoTrans.getInstance().amountWithDecimal(amount)
            history.time = Calendar.getInstance().timeInMillis / 1000L
            showResultUI(txResponse)
            hideLoading()
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
        MozoSDK.getInstance().profileViewModel.run {
            balanceAndRateLiveData.observeForever(balanceAndRateObserver)
        }

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
                    output_amount.setText("0$this")
                    output_amount.setSelection(this.length + 1)
                    return@run
                }
                launch(UI) {
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
            IntentIntegrator(this)
                    .setBeepEnabled(true)
                    .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                    .setPrompt("")
                    .initiateScan()
        }
        button_submit.click {
            if (output_receiver_address.isEnabled) {
                if (validateInput()) showConfirmationUI()
            } else {
                SecurityActivity.start(this, SecurityActivity.KEY_VERIFY_PIN_FOR_SEND, KEY_VERIFY_PIN)
            }
        }
    }

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> {
        it?.run {
            currentBalance = balanceInDecimal
            currentRate = rate
            val str = SpannableString(getString(R.string.mozo_transfer_spendable, currentBalance.displayString()))
            str.setSpan(
                    ForegroundColorSpan(color(R.color.mozo_color_primary)),
                    10,
                    str.length,
                    SpannableString.SPAN_INCLUSIVE_EXCLUSIVE
            )
            text_spendable.text = str
            output_amount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(12, decimal))
        }
    }

    private val contactsObserver = Observer<List<Models.Contact>> {
        it?.run {

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
            text_receiver_user_address.text = soloAddress

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

    private fun showResultUI(txResponse: Models.TransactionResponse?) = async(UI) {
        if (txResponse != null) {
            setContentView(R.layout.view_transaction_sent)
            button_close_transfer.click { finishAndRemoveTask() }

            text_send_complete_msg.text = txResponse.tx.hash

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

            updateTxStatusJob = updateTxStatus()
        } else {
            // TODO show send Tx failed UI
            "send Tx failed UI".logAsError()
        }
    }

    private fun updateTxStatus() = async {
        var pendingStatus = true
        while (pendingStatus) {

            val txStatus = MozoTrans.getInstance().getTransactionStatus(
                    this@TransactionFormActivity,
                    history.txHash ?: ""
            ) {
                updateTxStatus()
            }.await() ?: break

            launch(UI) {
                when {
                    txStatus.isSuccess() -> {
                        text_tx_status_icon.setImageResource(R.drawable.ic_check_green)
                        text_tx_status_label.setText(R.string.mozo_view_text_tx_success)
                        button_transaction_detail.visible()
                        pendingStatus = false
                        updateTxStatusJob = null
                    }
                    txStatus.isFailed() -> {
                        text_tx_status_icon.setImageResource(R.drawable.ic_error)
                        text_tx_status_label.setText(R.string.mozo_view_text_tx_failed)
                        pendingStatus = false
                        updateTxStatusJob = null
                    }
                }

                if (!pendingStatus) {
                    text_tx_status_loading.gone()
                    text_tx_status_icon.visible()
                }
            }

            delay(1500)
        }
    }

    private fun updateSubmitButton() {
        button_submit.isEnabled = (selectedContact != null || output_receiver_address.length() > 0) && output_amount.length() > 0
    }

    private fun showLoading() = async(UI) {
        loading_container.visible()
    }

    private fun hideLoading() = async(UI) {
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
        val address = selectedContact?.soloAddress ?: output_receiver_address.text.toString()
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

        fun start(context: Context) {
            Intent(context, TransactionFormActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(this)
            }
        }
    }
}