package io.mozocoin.sdk.transaction

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.google.zxing.integration.android.IntentIntegrator
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.common.model.TransactionHistory
import io.mozocoin.sdk.common.model.TransactionHistory.Companion.MY_ADDRESS
import io.mozocoin.sdk.common.model.TransactionResponse
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.contact.AddressAddActivity
import io.mozocoin.sdk.contact.AddressBookActivity
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.*
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
    private val history = TransactionHistory(
            txHash = "",
            blockHeight = 0L,
            action = "",
            fees = 0.0,
            amount = BigDecimal.ZERO,
            addressFrom = MY_ADDRESS,
            addressTo = "",
            contractAddress = "",
            symbol = "",
            contractAction = "",
            decimal = 2,
            time = 0L,
            txStatus = ""
    )
    private var updateTxStatusJob: Job? = null

    private var isViewOnlyMode = false
    private var isNeedBack2Edit = false
    private var mInputAmount = BigDecimal.ZERO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_transaction_form)

        MozoSDK.getInstance().contactViewModel.fetchData(this)

        initUI()
        showInputUI()

        MozoSDK.getInstance().profileViewModel.run {
            balanceAndRateLiveData.observe(this@TransactionFormActivity, balanceAndRateObserver)
            fetchBalance(this@TransactionFormActivity)
        }

        val address = intent?.getStringExtra(KEY_DATA_ADDRESS)
        val amount = intent?.getStringExtra(KEY_DATA_AMOUNT)
        if (address != null && amount != null) {
            isViewOnlyMode = true
            selectedContact = MozoSDK.getInstance().contactViewModel.findByAddress(address)
            output_receiver_address.setText(address)
            output_amount.setText(amount)
            mInputAmount = amount.toBigDecimal()

            showContactInfoUI()
            showConfirmationUI()
            button_submit.performClick()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MozoSDK.getInstance().contactViewModel.usersLiveData.removeObserver(userContactsObserver)
        MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.removeObserver(
                balanceAndRateObserver)
        selectedContact = null
        updateTxStatusJob?.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when {
            requestCode == KEY_PICK_ADDRESS -> {
                data?.run {
                    selectedContact = getParcelableExtra(AddressBookActivity.KEY_SELECTED_ADDRESS)
                    showContactInfoUI()
                }
            }
            data != null -> {
                IntentIntegrator
                        .parseActivityResult(requestCode, resultCode, data)
                        .contents?.let {
                    selectedContact = MozoSDK.getInstance().contactViewModel.findByAddress(it)

                    showInputUI()
                    if (selectedContact == null) {
                        output_receiver_address?.setText(it)
                        output_receiver_address?.dismissDropDown()
                        validateInput(true)
                    } else
                        showContactInfoUI()
                }
            }
        }
    }

    private fun sendTx() {
        val address = selectedContact?.soloAddress ?: output_receiver_address.text.toString()

        showLoading()
        MozoTx.getInstance()
                .createTransaction(this, address, mInputAmount.toString()) { response, doRetry ->
                    if (doRetry) {
                        showLoading()
                        sendTx()
                    } else {
                        hideLoading()
                        history.addressTo = address
                        history.amount = MozoTx.getInstance().amountWithDecimal(mInputAmount)
                        history.time = Calendar.getInstance().timeInMillis / 1000L
                        showResultUI(response)
                    }
                }
    }

    override fun onBackPressed() {
        when {
            isNeedBack2Edit && !isViewOnlyMode -> {
                showInputUI()
                showContactInfoUI()
            }
            else -> super.onBackPressed()
        }
    }

    private fun initUI() {
        transfer_container?.click {
            it.hideKeyboard()
        }

        output_receiver_address?.apply {
            onTextChanged {
                hideErrorAddressUI()
                updateSubmitButton()
            }
            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                output_receiver_address_label?.isSelected = hasFocus
                output_receiver_address_underline?.isSelected = hasFocus
                if (hasFocus && output_receiver_address?.length() ?: 0 > 0) {
                    output_receiver_address?.showDropDown()
                }
            }
            setOnClickListener {
                if (output_receiver_address?.length() ?: 0 > 0)
                    output_receiver_address?.showDropDown()
            }

            /**
             * Setup suggestion contact list
             */
            threshold = 1
            setOnItemClickListener { _, _, position, _ ->
                (adapter.getItem(position) as? Contact)?.let {
                    selectedContact = it
                    showContactInfoUI()
                    output_receiver_address?.text = null
                    output_amount?.requestFocus()
                }
            }
            setAdapter(ContactSuggestionAdapter(
                    context,
                    MozoSDK.getInstance().contactViewModel.contacts(),
                    onFindInSystemClick
            ))

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val value = output_receiver_address?.text?.toString()?.trim()
                    if (!value.isNullOrEmpty() && !WalletUtils.isValidAddress(value)) {
                        onFindInSystemClick.invoke()
                        return@setOnEditorActionListener true
                    }
                }
                false
            }
        }

        output_amount?.onAmountInputChanged(
                textChanged = {
                    hideErrorAmountUI()
                    updateSubmitButton()
                    if (it.isNullOrEmpty()) {
                        output_amount_rate?.text = ""
                        text_preview_rate?.text = ""
                    }
                },
                amountChanged = { amount ->
                    mInputAmount = amount
                    output_amount_rate?.text = MozoWallet.getInstance().amountInCurrency(amount)
                    text_preview_rate?.text =
                            MozoSDK.getInstance().profileViewModel.formatCurrencyDisplay(amount.multiply(
                                    currentRate), true)
                }
        )

        output_amount?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            output_amount_label?.isSelected = hasFocus
            output_amount_underline?.isSelected = hasFocus
        }

        transfer_toolbar?.onBackPress = { onBackPressed() }
        button_address_book?.click { AddressBookActivity.startForResult(this, KEY_PICK_ADDRESS) }
        button_scan_qr?.click {
            Support.scanQRCode(this)
        }
        button_submit?.click {
            if (output_receiver_address.isEnabled) {
                if (validateInput()) {
                    MozoTx.getInstance().verifyAddress(
                            it.context,
                            selectedContact?.soloAddress ?: output_receiver_address.text.toString()
                    ) { isValid ->
                        if (isValid) showConfirmationUI()
                    }
                }
            } else {
                MozoAPIsService.getInstance().checkNetworkStatus(this, { status, _ ->
                    status ?: return@checkNetworkStatus
                    sendTx()
                }, {
                    button_submit?.performClick()
                })
            }
        }
    }

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> { bar ->
        bar?.run {
            currentBalance = balanceNonDecimal
            currentRate = rate
            Support.formatSpendableText(text_spendable, currentBalance.displayString())

            if (isViewOnlyMode) {
                text_preview_rate?.text =
                        MozoSDK.getInstance().profileViewModel.formatCurrencyDisplay(
                                mInputAmount.multiply(currentRate),
                                true
                        )
            }
        }
    }

    private fun showInputUI() {
        output_receiver_address?.isEnabled = true
        output_amount?.isEnabled = true
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

        transfer_toolbar?.showBackButton(false)
        button_submit?.setText(R.string.mozo_button_continue)

        if (output_amount_error_msg?.visibility != View.VISIBLE) {
            text_spendable?.visible()
        }
    }

    private fun showContactInfoUI() {
        hideErrorAddressUI()
        updateSubmitButton()
        selectedContact?.run {
            output_receiver_address?.visibility = View.INVISIBLE
            output_receiver_address_underline?.visibility = View.INVISIBLE
            button_scan_qr?.visibility = View.INVISIBLE

            output_receiver_address_user?.visible()
            output_receiver_icon?.setImageResource(if (isStore) R.drawable.ic_store else R.drawable.ic_receiver)
            text_receiver_phone?.text = if (isStore) physicalAddress else phoneNo
            text_receiver_phone?.isVisible = text_receiver_phone?.length() ?: 0 > 0

            text_receiver_user_name?.text = name
            text_receiver_user_name?.isVisible = !name.isNullOrEmpty()
            text_receiver_user_address?.text = soloAddress

            button_clear?.apply {
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
        isNeedBack2Edit = true
        output_receiver_address?.isEnabled = false
        output_receiver_address?.setSelection(0)

        output_amount?.isEnabled = false
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
        text_preview_amount?.text = mInputAmount.displayString()
        visible(arrayOf(
                send_state_container,
                confirmation_state_separator,
                output_amount_preview_container
        ))

        transfer_toolbar?.showBackButton(true)
        button_submit?.setText(R.string.mozo_button_send)
    }

    private fun updateContactUI() {
        button_save_address?.isVisible = selectedContact == null
        val contact = selectedContact

        if (contact == null) {
            text_preview_address_sent?.text = history.addressTo
            output_receiver_address_user_sent?.gone()
            return
        }

        output_receiver_address_user_sent?.visible()
        output_receiver_icon_sent?.setImageResource(if (contact.isStore) R.drawable.ic_store else R.drawable.ic_receiver)

        text_preview_address_sent?.gone()
        text_receiver_user_name_sent?.text = contact.name
        text_receiver_user_name_sent?.isVisible = !contact.name.isNullOrEmpty()
        text_receiver_user_address_sent?.text = contact.soloAddress
        text_receiver_phone_sent?.text = if (contact.isStore) contact.physicalAddress else contact.phoneNo
        text_receiver_phone_sent?.isVisible = text_receiver_phone_sent?.length() ?: 0 > 0
    }

    private val userContactsObserver = Observer<List<Contact>?> {
        selectedContact = MozoSDK.getInstance().contactViewModel.findByAddress(history.addressTo)
        updateContactUI()
    }

    private fun showResultUI(txResponse: TransactionResponse?) = GlobalScope.launch(Dispatchers.Main) {
        if (txResponse != null) {
            MozoSDK.getInstance()
                    .contactViewModel.usersLiveData.observe(this@TransactionFormActivity,
                    userContactsObserver)
            setContentView(R.layout.view_transaction_sent)

            transfer_completed_title.setText(R.string.mozo_transfer_action_complete)
            text_preview_amount_sent.text = history.amountDisplay()
            text_preview_rate_sent.text = MozoSDK.getInstance()
                    .profileViewModel.formatCurrencyDisplay(history.amountInDecimal().multiply(
                    currentRate), true)

            button_save_address?.apply {
                isVisible = selectedContact == null
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

            updateContactUI()
            updateTxStatus()
        }
    }

    private fun updateTxStatus() {
        MozoTx.getInstance().getTransactionStatus(this, history.txHash ?: return) {
            when {
                it.isSuccess() -> {
                    transfer_completed_title.setText(R.string.mozo_transfer_send_complete)
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
        button_submit?.isEnabled =
                (selectedContact != null || output_receiver_address.length() > 0) && output_amount.length() > 0
    }

    private fun showLoading() = GlobalScope.launch(Dispatchers.Main) {
        transfer_loading_container?.visible()
    }

    private fun hideLoading() = GlobalScope.launch(Dispatchers.Main) {
        transfer_loading_container?.gone()
    }

    private fun showErrorAddressUI(fromScan: Boolean = false, @StringRes errorId: Int = R.string.mozo_transfer_receiver_address_error) {
        val errorColor = color(R.color.mozo_color_error)
        output_receiver_address_label?.setTextColor(errorColor)
        output_receiver_address_underline?.setBackgroundColor(errorColor)
        output_receiver_address_error_msg?.visible()
        output_receiver_address_error_msg?.setText(
                if (fromScan) R.string.mozo_dialog_error_scan_invalid_msg else errorId
        )
    }

    private fun hideErrorAddressUI() {
        output_receiver_address_label?.setTextColor(ContextCompat.getColorStateList(this,
                R.color.mozo_color_input_focus))
        output_receiver_address_underline?.setBackgroundResource(R.drawable.mozo_color_line_focus)
        output_receiver_address_error_msg?.gone()
    }

    private fun showErrorAmountUI(@StringRes errorId: Int? = null) {
        val errorColor = color(R.color.mozo_color_error)
        output_amount_label?.setTextColor(errorColor)
        output_amount_underline?.setBackgroundColor(errorColor)
        output_amount_error_msg?.setText(errorId ?: R.string.mozo_transfer_amount_error)
        output_amount_error_msg?.visible()
        text_spendable?.gone()
    }

    private fun hideErrorAmountUI() {
        output_amount_label?.setTextColor(ContextCompat.getColorStateList(this,
                R.color.mozo_color_input_focus))
        output_amount_underline?.setBackgroundResource(R.drawable.mozo_color_line_focus)
        output_amount_error_msg?.gone()
        text_spendable?.visible()
    }

    @SuppressLint("SetTextI18n")
    private fun validateInput(fromScan: Boolean = false): Boolean {
        var isValidAddress = true
        val address = selectedContact?.soloAddress ?: output_receiver_address.text.toString()
        if (!WalletUtils.isValidAddress(address)) {
            if (fromScan) output_receiver_address.text = null
            showErrorAddressUI(fromScan)
            isValidAddress = false
        }

        if (fromScan) return isValidAddress

        if (!isValidAddress) {
            when {
                address.isDigitsOnly() -> {
                    showErrorAddressUI(false, R.string.mozo_transfer_amount_error_invalid_phone)
                }
                address.startsWith("+") -> {
                    if (MozoSDK.getInstance().contactViewModel.containCountryCode(address)) {
                        if (!address.isValidPhone(this))
                            showErrorAddressUI(false, R.string.mozo_transfer_amount_error_invalid_phone)

                    } else showErrorAddressUI(false, R.string.mozo_transfer_amount_error_invalid_country_code)
                }
            }
        }

        var isValidAmount = true
        when {
            mInputAmount > currentBalance -> {
                showErrorAmountUI(R.string.mozo_transfer_amount_error_not_enough)
                isValidAmount = false
            }
            mInputAmount <= BigDecimal.ZERO -> {
                showErrorAmountUI(R.string.mozo_transfer_amount_error_too_low)
                isValidAmount = false
            }
        }

        return isValidAddress && isValidAmount
    }

    private val onFindInSystemClick: () -> Unit = {
        val value = output_receiver_address?.text?.toString()?.trim()
        if (!value.isNullOrEmpty()) {
            when {
                value.isDigitsOnly() -> {
                    MessageDialog.show(this, getString(R.string.mozo_transfer_amount_error_invalid_phone).split(": ")[1])
                }
                value.startsWith("+") -> {
                    if (MozoSDK.getInstance().contactViewModel.containCountryCode(value)) {
                        if (value.isValidPhone(this)) findContact(value)
                        else MessageDialog.show(
                                this,
                                getString(R.string.mozo_transfer_amount_error_invalid_phone)
                                        .split(": ")[1]
                        )
                    } else MessageDialog.show(
                            this,
                            getString(R.string.mozo_transfer_amount_error_invalid_country_code)
                                    .split(": ")[1]
                    )
                }
                else -> MessageDialog.show(this, R.string.mozo_transfer_contact_find_err)
            }
        } else {
            MessageDialog.show(this, R.string.mozo_transfer_contact_find_err)
        }
    }

    private fun findContact(phone: String) {
        MozoAPIsService.getInstance().findContact(this, phone, { data, _ ->
            if (data?.soloAddress.isNullOrEmpty()) {
                MessageDialog.show(this, R.string.mozo_transfer_contact_find_no_address)

            } else {
                selectedContact = data
                showContactInfoUI()
            }
        }, {
            findContact(phone)
        })
    }

    companion object {
        private const val KEY_PICK_ADDRESS = 0x0021
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