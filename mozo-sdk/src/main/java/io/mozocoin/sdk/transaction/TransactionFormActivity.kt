package io.mozocoin.sdk.transaction

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.google.zxing.integration.android.IntentIntegrator
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.common.model.TransactionHistory
import io.mozocoin.sdk.common.model.TransactionHistory.Companion.MY_ADDRESS
import io.mozocoin.sdk.common.model.TransactionResponse
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.contact.AddressAddActivity
import io.mozocoin.sdk.contact.AddressBookActivity
import io.mozocoin.sdk.databinding.ViewTransactionFormBinding
import io.mozocoin.sdk.databinding.ViewTransactionSentBinding
import io.mozocoin.sdk.ui.BaseActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*
import org.web3j.crypto.WalletUtils
import java.math.BigDecimal
import java.util.*

@Suppress("unused")
internal class TransactionFormActivity : BaseActivity() {

    private lateinit var bindingForm: ViewTransactionFormBinding
    private lateinit var bindingSent: ViewTransactionSentBinding
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

    private var mPhoneContactUtils: PhoneContactUtils? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingForm = ViewTransactionFormBinding.inflate(layoutInflater)
        bindingSent = ViewTransactionSentBinding.inflate(layoutInflater)
        setContentView(bindingForm.root)

        MozoSDK.getInstance().contactViewModel.fetchData(this)
        mPhoneContactUtils = PhoneContactUtils(bindingForm.outputReceiverAddress) {
            selectedContact = it
            showContactInfoUI()
        }
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
            bindingForm.outputReceiverAddress.setText(address)
            bindingForm.outputAmount.setText(amount)
            mInputAmount = amount.toBigDecimal()

            showContactInfoUI()
            showConfirmationUI()
            bindingForm.buttonSubmit.performClick()
        }
    }

    override fun onDestroy() {
        MozoSDK.getInstance().contactViewModel.usersLiveData.removeObserver(userContactsObserver)
        MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.removeObserver(
                balanceAndRateObserver)
        mPhoneContactUtils = null
        selectedContact = null
        updateTxStatusJob?.cancel()
        super.onDestroy()
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
                                bindingForm.outputReceiverAddress.setText(it)
                                bindingForm.outputReceiverAddress.dismissDropDown()
                                validateInput(true)
                            } else
                                showContactInfoUI()
                        }
            }
        }
    }

    private fun sendTx() {
        val address = selectedContact?.soloAddress
                ?: bindingForm.outputReceiverAddress.text.toString()

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
        bindingForm.transferContainer.click {
            it.hideKeyboard()
        }

        bindingForm.outputReceiverAddress.apply {
            onTextChanged {
                hideErrorAddressUI()
                updateSubmitButton()
            }
            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                bindingForm.outputReceiverAddressLabel.isSelected = hasFocus
                bindingForm.outputReceiverAddressUnderline.isSelected = hasFocus
                if (hasFocus && bindingForm.outputReceiverAddress.length() > 0) {
                    bindingForm.outputReceiverAddress.showDropDown()
                }
            }
            setOnClickListener {
                if (bindingForm.outputReceiverAddress.length() > 0)
                    bindingForm.outputReceiverAddress.showDropDown()
            }

            /**
             * Setup suggestion contact list
             */
            threshold = 1
            setOnItemClickListener { _, _, position, _ ->
                (adapter.getItem(position) as? Contact)?.let {
                    selectedContact = it
                    showContactInfoUI()
                    bindingForm.outputReceiverAddress.text = null
                    bindingForm.outputAmount.requestFocus()
                }
            }
            setAdapter(ContactSuggestionAdapter(
                    context,
                    MozoSDK.getInstance().contactViewModel.contacts(),
                    mPhoneContactUtils?.onFindInSystemClick
            ))

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val value = bindingForm.outputReceiverAddress.text?.toString()?.trim()
                    if (!value.isNullOrEmpty() && !WalletUtils.isValidAddress(value)) {
                        mPhoneContactUtils?.onFindInSystemClick?.invoke()
                        return@setOnEditorActionListener true
                    }
                }
                false
            }
        }

        bindingForm.outputAmount.onAmountInputChanged(
                textChanged = {
                    hideErrorAmountUI()
                    updateSubmitButton()
                    if (it.isNullOrEmpty()) {
                        bindingForm.outputAmountRate.text = ""
                        bindingForm.textPreviewRate.text = ""
                    }
                },
                amountChanged = { amount ->
                    mInputAmount = amount
                    bindingForm.outputAmountRate.text = MozoWallet.getInstance().amountInCurrency(amount)
                    bindingForm.textPreviewRate.text = MozoSDK.getInstance().profileViewModel
                            .formatCurrencyDisplay(
                                    amount.multiply(currentRate),
                                    true
                            )
                }
        )

        bindingForm.outputAmount.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            bindingForm.outputAmountLabel.isSelected = hasFocus
            bindingForm.outputAmountUnderline.isSelected = hasFocus
        }
        bindingForm.outputAmountRate.alpha = if (Constant.SHOW_MOZO_EQUIVALENT_CURRENCY) 1f else 0f
        bindingForm.textPreviewRate.alpha = if (Constant.SHOW_MOZO_EQUIVALENT_CURRENCY) 1f else 0f

        bindingForm.transferToolbar.onBackPress = { onBackPressed() }
        bindingForm.buttonAddressBook.click { AddressBookActivity.startForResult(this, KEY_PICK_ADDRESS) }
        bindingForm.buttonScanQr.click {
            Support.scanQRCode(this)
        }
        bindingForm.buttonSubmit.click {
            if (bindingForm.outputReceiverAddress.isEnabled) {
                if (validateInput()) {
                    val output = selectedContact?.soloAddress
                            ?: bindingForm.outputReceiverAddress.text.toString()
                    when (output) {
                        MozoWallet.getInstance().getAddress() -> {
                            MessageDialog.show(it.context, R.string.mozo_transfer_err_send_to_own_wallet)
                        }
                        else -> MozoTx.getInstance().verifyAddress(it.context, output) { isValid ->
                            if (isValid) showConfirmationUI()
                        }
                    }
                }
            } else {
                MozoAPIsService.getInstance().checkNetworkStatus(this, { status, _ ->
                    status ?: return@checkNetworkStatus
                    sendTx()
                }, {
                    bindingForm.buttonSubmit.performClick()
                })
            }
        }
    }

    private val balanceAndRateObserver = Observer<ViewModels.BalanceAndRate?> { bar ->
        bar?.run {
            currentBalance = balanceNonDecimal
            currentRate = rate
            Support.formatSpendableText(bindingForm.textSpendable, currentBalance.displayString())

            if (isViewOnlyMode) {
                bindingForm.textPreviewRate.text = MozoSDK.getInstance().profileViewModel
                        .formatCurrencyDisplay(
                                mInputAmount.multiply(currentRate),
                                true
                        )
            }
        }
    }

    private fun showInputUI() {
        bindingForm.outputReceiverAddress.isEnabled = true
        bindingForm.outputAmount.isEnabled = true
        visible(
                bindingForm.outputReceiverAddress,
                bindingForm.outputReceiverAddressUnderline,
                bindingForm.buttonAddressBook,
                bindingForm.buttonScanQr,
                bindingForm.outputAmount,
                bindingForm.outputAmountRate,
                bindingForm.outputAmountUnderline
        )
        gone(
                bindingForm.outputReceiverAddressUser,
                bindingForm.sendStateContainer,
                bindingForm.confirmationStateSeparator,
                bindingForm.outputAmountPreviewContainer
        )

        bindingForm.transferToolbar.showBackButton(false)
        bindingForm.buttonSubmit.setText(R.string.mozo_button_continue)

        if (bindingForm.outputAmountErrorMsg.visibility != View.VISIBLE) {
            bindingForm.textSpendable.visible()
        }
    }

    private fun showContactInfoUI() {
        hideErrorAddressUI()
        updateSubmitButton()
        selectedContact?.run {
            bindingForm.outputReceiverAddress.visibility = View.INVISIBLE
            bindingForm.outputReceiverAddressUnderline.visibility = View.INVISIBLE
            bindingForm.buttonScanQr.visibility = View.INVISIBLE

            bindingForm.outputReceiverAddressUser.visible()
            bindingForm.outputReceiverIcon.setImageResource(if (isStore) R.drawable.ic_store else R.drawable.ic_receiver)
            bindingForm.textReceiverPhone.text = if (isStore) physicalAddress else phoneNo
            bindingForm.textReceiverPhone.isVisible = bindingForm.textReceiverPhone.length() > 0

            bindingForm.textReceiverUserName.text = name
            bindingForm.textReceiverUserName.isVisible = !name.isNullOrEmpty()
            bindingForm.textReceiverUserAddress.text = soloAddress

            bindingForm.outputAmount.requestFocus()

            bindingForm.buttonClear.apply {
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
        bindingForm.outputReceiverAddress.isEnabled = false
        bindingForm.outputReceiverAddress.setSelection(0)

        bindingForm.outputAmount.isEnabled = false
        gone(
                bindingForm.outputReceiverAddressUnderline,
                bindingForm.buttonAddressBook,
                bindingForm.buttonScanQr,
                bindingForm.outputAmount,
                bindingForm.outputAmountRate,
                bindingForm.outputAmountUnderline,
                bindingForm.textSpendable,
                bindingForm.buttonClear
        )
        bindingForm.textPreviewAmount.text = mInputAmount.displayString()
        visible(
                bindingForm.sendStateContainer,
                bindingForm.confirmationStateSeparator,
                bindingForm.outputAmountPreviewContainer
        )

        bindingForm.transferToolbar.showBackButton(true)
        bindingForm.buttonSubmit.setText(R.string.mozo_button_send)
        bindingForm.buttonSubmit.isClickable = true
    }

    private val userContactsObserver = Observer<List<Contact>?> {
        selectedContact = MozoSDK.getInstance().contactViewModel.findByAddress(history.addressTo)
        updateContactUI()
    }

    private fun showResultUI(txResponse: TransactionResponse?) = MainScope().launch {
        if (txResponse != null) {
            MozoSDK.getInstance().contactViewModel.usersLiveData.observe(
                    this@TransactionFormActivity,
                    userContactsObserver
            )
            setContentView(bindingSent.root)

            bindingSent.transferCompletedTitle.setText(R.string.mozo_transfer_action_complete)
            bindingSent.textPreviewAmountSent.text = history.amountDisplay()
            bindingSent.textPreviewRateSent.text = MozoSDK.getInstance().profileViewModel
                    .formatCurrencyDisplay(
                            history.amountInDecimal().multiply(currentRate),
                            true
                    )

            bindingSent.buttonSaveAddress.apply {
                isVisible = selectedContact == null
                click {
                    AddressAddActivity.start(this@TransactionFormActivity, history.addressTo)
                }
            }

            history.txHash = txResponse.tx.hash ?: ""

            bindingSent.buttonTransactionDetail.apply {
                gone()
                click {
                    TransactionDetailsActivity.start(this@TransactionFormActivity, history)
                }
            }

            updateContactUI()
            updateTxStatus()
        }
    }

    private fun updateContactUI() {
        bindingSent.buttonSaveAddress.isVisible = selectedContact == null
        val contact = selectedContact

        if (contact == null) {
            bindingSent.textPreviewAddressSent.text = history.addressTo
            bindingSent.outputReceiverAddressUserSent.gone()
            return
        }

        bindingSent.outputReceiverAddressUserSent.visible()
        bindingSent.outputReceiverIconSent.setImageResource(
                if (contact.isStore) R.drawable.ic_store else R.drawable.ic_receiver
        )

        bindingSent.textPreviewAddressSent.gone()
        bindingSent.textReceiverUserNameSent.text = contact.name
        bindingSent.textReceiverUserNameSent.isVisible = !contact.name.isNullOrEmpty()
        bindingSent.textReceiverUserAddressSent.text = contact.soloAddress
        bindingSent.textReceiverPhoneSent.text = if (contact.isStore) contact.physicalAddress else contact.phoneNo
        bindingSent.textReceiverPhoneSent.isVisible = bindingSent.textReceiverPhoneSent.length() > 0
    }

    private fun updateTxStatus() {
        MozoTx.getInstance().getTransactionStatus(this, history.txHash ?: return) {
            when {
                it.isSuccess() -> {
                    bindingSent.transferCompletedTitle.setText(R.string.mozo_transfer_send_complete)
                    bindingSent.transferInfoContainer.visible()
                    bindingSent.transferStatusContainer.gone()
                    bindingSent.buttonTransactionDetail.visible()
                }
                it.isFailed() -> {
                    bindingSent.textTxStatusIcon.setImageResource(R.drawable.ic_error)
                    bindingSent.textTxStatusLabel.setText(R.string.mozo_view_text_tx_failed)
                }
            }

            updateTxStatusJob = if (it.isSuccess() || it.isFailed()) {
                bindingSent.textTxStatusLoading.gone()
                bindingSent.textTxStatusIcon.visible()
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
        bindingForm.buttonSubmit.isEnabled = (selectedContact != null || bindingForm.outputReceiverAddress.length() > 0)
                && bindingForm.outputAmount.length() > 0
    }

    private fun showLoading() = MainScope().launch {
        bindingForm.transferLoadingContainer.visible()
    }

    private fun hideLoading() = MainScope().launch {
        bindingForm.transferLoadingContainer.gone()
    }

    private fun showErrorAddressUI(fromScan: Boolean = false, @StringRes errorId: Int = R.string.mozo_transfer_receiver_address_error) {
        val errorColor = color(R.color.mozo_color_error)
        bindingForm.outputReceiverAddressLabel.setTextColor(errorColor)
        bindingForm.outputReceiverAddressUnderline.setBackgroundColor(errorColor)
        bindingForm.outputReceiverAddressErrorMsg.visible()
        bindingForm.outputReceiverAddressErrorMsg.setText(
                if (fromScan) R.string.mozo_dialog_error_scan_invalid_msg else errorId
        )
    }

    private fun hideErrorAddressUI() {
        bindingForm.outputReceiverAddressLabel.setTextColor(ContextCompat.getColorStateList(this,
                R.color.mozo_color_input_focus))
        bindingForm.outputReceiverAddressUnderline.setBackgroundResource(R.drawable.mozo_color_line_focus)
        bindingForm.outputReceiverAddressErrorMsg.gone()
    }

    private fun showErrorAmountUI(@StringRes errorId: Int? = null) {
        val errorColor = color(R.color.mozo_color_error)
        bindingForm.outputAmountLabel.setTextColor(errorColor)
        bindingForm.outputAmountUnderline.setBackgroundColor(errorColor)
        bindingForm.outputAmountErrorMsg.setText(errorId ?: R.string.mozo_transfer_amount_error)
        bindingForm.outputAmountErrorMsg.visible()
        bindingForm.textSpendable.gone()
    }

    private fun hideErrorAmountUI() {
        bindingForm.outputAmountLabel.setTextColor(ContextCompat.getColorStateList(this,
                R.color.mozo_color_input_focus))
        bindingForm.outputAmountUnderline.setBackgroundResource(R.drawable.mozo_color_line_focus)
        bindingForm.outputAmountErrorMsg.gone()
        bindingForm.textSpendable.visible()
    }

    @SuppressLint("SetTextI18n")
    private fun validateInput(fromScan: Boolean = false): Boolean {
        var isValidAddress = true
        val address = selectedContact?.soloAddress
                ?: bindingForm.outputReceiverAddress.text.toString()
        if (!WalletUtils.isValidAddress(address)) {
            if (fromScan) bindingForm.outputReceiverAddress.text = null
            showErrorAddressUI(fromScan)
            isValidAddress = false
        }

        if (fromScan) return isValidAddress

        if (!isValidAddress) mPhoneContactUtils?.validatePhone(this, address)?.let {
            if (it > 0) showErrorAddressUI(false, it)
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