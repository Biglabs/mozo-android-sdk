package io.mozocoin.sdk.transaction.payment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.zxing.integration.android.IntentIntegrator
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.common.model.PaymentRequest
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.contact.AddressBookActivity
import io.mozocoin.sdk.transaction.ContactSuggestionAdapter
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.*
import kotlinx.android.synthetic.main.fragment_payment_send.*
import kotlinx.coroutines.*
import org.web3j.crypto.WalletUtils
import java.math.BigDecimal

class PaymentRequestSendFragment : Fragment() {

    private var mListener: PaymentRequestInteractionListener? = null
    private var generateQRJob: Job? = null
    private var amountBigDecimal = BigDecimal.ZERO
    private var selectedContact: Contact? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PaymentRequestActivity) {
            mListener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_payment_send, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myAddress = MozoWallet.getInstance().getAddress()
        myAddress ?: return

        val amount = arguments?.getString(KEY_AMOUNT)
        amount ?: return

        val content = "mozox:$myAddress?amount=$amount"
        generateQRJob = GlobalScope.launch {
            val size = view.context.resources.dp2Px(177f).toInt()
            val qrImage = Support.generateQRCode(content, size)
            withContext(Dispatchers.Main) {
                payment_request_qr_image?.setImageBitmap(qrImage)
            }
        }

        amountBigDecimal = amount.toBigDecimal()
        payment_request_amount.text = amountBigDecimal.displayString()

        output_receiver_address.onTextChanged {
            updateSubmitButton()
        }

        output_receiver_address?.apply {
            onTextChanged {
                hideErrorAddressUI()
                updateSubmitButton()
            }

            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                output_receiver_address_label?.isSelected = hasFocus
                output_receiver_address_underline?.isSelected = hasFocus
                if (hasFocus && output_receiver_address?.length() ?: 0 > 0)
                    output_receiver_address?.showDropDown()
            }

            setOnClickListener {
                if (output_receiver_address?.length() ?: 0 > 0)
                    output_receiver_address?.showDropDown()
            }

            threshold = 1
            setOnItemClickListener { _, _, position, _ ->
                (adapter.getItem(position) as? Contact)?.let {
                    selectedContact = it
                    showContactInfoUI()
                    output_receiver_address?.text = null
                    output_receiver_address?.clearFocus()
                    output_receiver_address?.hideKeyboard()
                }
            }
            setAdapter(ContactSuggestionAdapter(
                context,
                MozoSDK.getInstance().contactViewModel.contacts(),
                onFindInSystemClick
            ))
        }

        button_scan_qr.click {
            Support.scanQRCode(this@PaymentRequestSendFragment)
        }

        button_address_book.click {
            AddressBookActivity.startForResult(this, KEY_PICK_ADDRESS)
        }

        button_send.click {
            if (!validateInput()) {
                showErrorAddressUI()
                return@click
            }

            mListener?.onSendRequestClicked(
                amount,
                (selectedContact?.soloAddress
                    ?: output_receiver_address.text.toString()).toLowerCase(),
                PaymentRequest(content = content)
            )
        }

        MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.observe(
            this,
            Observer<ViewModels.BalanceAndRate?> {
                it ?: return@Observer
                payment_request_rate.text = MozoSDK.getInstance().profileViewModel
                    .formatCurrencyDisplay(
                        amountBigDecimal.multiply(it.rate),
                        true
                    )
            }
        )
    }

    private val onFindInSystemClick: () -> Unit = {
        context?.let {
            val value = output_receiver_address?.text?.toString()?.trim()
            if (!value.isNullOrEmpty() && value.isValidPhone(it)) {
                findContact(value)

            } else {
                MessageDialog.show(
                    it,
                    R.string.mozo_transfer_contact_find_err
                )
            }
        }
    }

    private fun findContact(phone: String) {
        val context = context ?: return
        MozoAPIsService.getInstance().findContact(context, phone, { data, _ ->
            if (data?.soloAddress.isNullOrEmpty()) {
                MessageDialog.show(context, R.string.mozo_transfer_contact_find_no_address)

            } else {
                selectedContact = data
                showContactInfoUI()
            }
        }, {
            findContact(phone)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != AppCompatActivity.RESULT_OK) return
        when {
            requestCode == KEY_PICK_ADDRESS -> {
                data?.run {
                    selectedContact = getParcelableExtra(AddressBookActivity.KEY_SELECTED_ADDRESS)
                    showContactInfoUI()
                    updateSubmitButton()
                }
            }
            data != null                    -> {
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data).contents?.let {
                    if (WalletUtils.isValidAddress(it)) {
                        selectedContact = MozoSDK.getInstance().contactViewModel.findByAddress(it)

                        showInputUI()
                        if (selectedContact == null) {
                            output_receiver_address?.setText(it)
                            output_receiver_address?.dismissDropDown()
                            validateInput(true)
                        } else
                            showContactInfoUI()
                        updateSubmitButton()
                    } else if (context != null) {
                        MessageDialog.show(context!!, R.string.mozo_dialog_error_scan_invalid_msg)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        generateQRJob?.cancel()
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

    private fun showInputUI() {
        output_receiver_address.visible()

        output_receiver_address_underline?.visible()
        button_scan_qr?.visible()
        output_receiver_address_user?.gone()
    }

    private fun updateSubmitButton() {
        button_send.isEnabled = selectedContact != null || output_receiver_address.length() > 0
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
                address.isDigitsOnly() && address.length < 20               -> {
                    showErrorAddressUI(false, R.string.mozo_transfer_amount_error_no_country_code)
                }
                address.startsWith("+") && !address.isValidPhone(context!!) -> {
                    showErrorAddressUI(false,
                        R.string.mozo_transfer_amount_error_invalid_country_code)
                }
            }
        }

        return isValidAddress
    }

    private fun showErrorAddressUI(fromScan: Boolean = false, @StringRes errorId: Int = R.string.mozo_transfer_receiver_address_error) {
        val errorColor = context?.color(R.color.mozo_color_error) ?: return
        output_receiver_address_label?.setTextColor(errorColor)
        output_receiver_address_underline?.setBackgroundColor(errorColor)
        output_receiver_address_error_msg?.visible()
        output_receiver_address_error_msg?.setText(
            if (fromScan) R.string.mozo_dialog_error_scan_invalid_msg else errorId
        )
    }

    private fun hideErrorAddressUI() {
        output_receiver_address_label?.setTextColor(
            ContextCompat.getColorStateList(context ?: return,
                R.color.mozo_color_input_focus)
        )
        output_receiver_address_underline?.setBackgroundResource(R.drawable.mozo_color_line_focus)
        output_receiver_address_error_msg?.gone()
    }

    companion object {

        private const val KEY_PICK_ADDRESS = 0x0021
        private const val KEY_AMOUNT = "key_amount"

        fun getInstance(amount: String) = PaymentRequestSendFragment().apply {
            arguments = Bundle().apply { putString(KEY_AMOUNT, amount) }
        }
    }
}