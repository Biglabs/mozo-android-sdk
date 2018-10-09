package com.biglabs.mozo.sdk.trans

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputFilter
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.core.Models
import com.biglabs.mozo.sdk.core.Models.TransactionHistory.CREATOR.MY_ADDRESS
import com.biglabs.mozo.sdk.services.AddressBookService
import com.biglabs.mozo.sdk.ui.AddressAddActivity
import com.biglabs.mozo.sdk.ui.AddressBookActivity
import com.biglabs.mozo.sdk.ui.SecurityActivity
import com.biglabs.mozo.sdk.utils.*
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.view_transaction_form.*
import kotlinx.android.synthetic.main.view_transaction_sent.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.web3j.crypto.WalletUtils
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@Suppress("unused")
internal class TransactionFormActivity : AppCompatActivity() {

    private var currentBalance = BigDecimal.ZERO
    private var selectedContact: Models.Contact? = null
    private val history = Models.TransactionHistory("", 0L, "", 0.0, BigDecimal.ZERO, MY_ADDRESS, "", "", "", "", 2, 0L, "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_transaction_form)

        initUI()
        showInputUI()

        mozo_wallet_balance_rate_side.text = "₩102.230"

        AddressBookService.getInstance().fetchData(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        selectedContact = null
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
                    selectedContact = AddressBookService.getInstance().findByAddress(it)

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
            val txResponse = MozoTrans.getInstance().createTransaction(address, amount, pin).await()
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
        launch {
            val balance = MozoTrans.getInstance().getBalance().await() ?: "0"
            currentBalance = BigDecimal(NumberFormat.getNumberInstance().parse(balance).toDouble())
            launch(UI) {
                mozo_wallet_balance_value?.text = balance

                val str = SpannableString(getString(R.string.mozo_transfer_spendable, balance))
                str.setSpan(
                        ForegroundColorSpan(color(R.color.mozo_color_title)),
                        0,
                        10,
                        SpannableString.SPAN_INCLUSIVE_EXCLUSIVE
                )
                text_spendable.text = str
            }
        }

        output_receiver_address.onTextChanged {
            hideErrorAddressUI()
            updateSubmitButton()
        }
        output_amount.onTextChanged {
            hideErrorAmountUI()
            updateSubmitButton()
        }

        val decimal = PreferenceUtils.getInstance(this).getDecimal()
        output_amount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(12, decimal))

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
                SecurityActivity.start(this, SecurityActivity.KEY_VERIFY_PIN, KEY_VERIFY_PIN)
            }
        }
    }

    private fun showInputUI() {
        output_receiver_address.isEnabled = true
        output_amount.isEnabled = true
        output_amount_label.setText(R.string.mozo_transfer_amount)
        visible(arrayOf(
                output_receiver_address,
                output_receiver_address_underline,
                button_address_book,
                button_scan_qr,
                output_amount,
                output_amount_underline
        ))
        gone(arrayOf(
                output_receiver_address_user,
                output_amount_preview_container
        ))

        transfer_toolbar.setTitle(R.string.mozo_transfer_title)
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
        output_amount_label.setText(R.string.mozo_transfer_amount_offchain)
        gone(arrayOf(
                output_receiver_address_underline,
                button_address_book,
                button_scan_qr,
                output_amount,
                output_amount_underline,
                text_spendable,
                button_clear
        ))
        text_preview_amount.text = output_amount.text
        output_amount_preview_container.visible()

        transfer_toolbar.setTitle(R.string.mozo_transfer_confirmation)
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
            button_transaction_detail.click {
                TransactionDetails.start(this@TransactionFormActivity, history)
            }
        } else {
            // TODO show send Tx failed UI
            "send Tx failed UI".logAsError()
        }
    }

    private fun updateSubmitButton() {
        button_submit.isEnabled = (selectedContact != null || output_receiver_address.length() > 0) && output_amount.length() > 0
    }

    private fun showLoading() = async(UI) {
        loading_container.show()
    }

    private fun hideLoading() = async(UI) {
        loading_container.hide()
    }

    private fun showErrorAddressUI() {
        val errorColor = color(R.color.mozo_color_error)
        output_receiver_address_label.setTextColor(errorColor)
        output_receiver_address_underline.setBackgroundColor(errorColor)
        output_receiver_address_error_msg.visible()
    }

    private fun hideErrorAddressUI() {
        output_receiver_address_label.setTextColor(color(R.color.mozo_color_content))
        output_receiver_address_underline.setBackgroundColor(color(R.color.mozo_color_un_active))
        output_receiver_address_error_msg.gone()
    }

    private fun showErrorAmountUI() {
        val errorColor = color(R.color.mozo_color_error)
        output_amount_label.setTextColor(errorColor)
        output_amount_underline.setBackgroundColor(errorColor)
        output_amount_error_msg.visible()
        text_spendable.gone()
    }

    private fun hideErrorAmountUI() {
        output_amount_label.setTextColor(color(R.color.mozo_color_content))
        output_amount_underline.setBackgroundColor(color(R.color.mozo_color_un_active))
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
        if (bigAmount <= BigDecimal.ZERO || bigAmount > currentBalance) {
            showErrorAmountUI()
            isValidAmount = false
        }

        return isValidAddress && isValidAmount
    }

    companion object {
        private const val KEY_PICK_ADDRESS = 0x0021
        private const val KEY_VERIFY_PIN = 0x0022

        fun start(context: Context) {
            Intent(context, TransactionFormActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                context.startActivity(this)
            }
        }
    }
}