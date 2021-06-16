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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.zxing.integration.android.IntentIntegrator
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.common.model.PaymentRequest
import io.mozocoin.sdk.contact.AddressBookActivity
import io.mozocoin.sdk.databinding.FragmentPaymentSendBinding
import io.mozocoin.sdk.transaction.ContactSuggestionAdapter
import io.mozocoin.sdk.ui.dialog.MessageDialog
import io.mozocoin.sdk.utils.*
import kotlinx.coroutines.*
import org.web3j.crypto.WalletUtils
import java.math.BigDecimal
import java.util.*

class PaymentRequestSendFragment : Fragment() {

    private var _binding: FragmentPaymentSendBinding? = null
    private val binding get() = _binding!!
    private var mListener: PaymentRequestInteractionListener? = null
    private var generateQRJob: Job? = null
    private var amountBigDecimal = BigDecimal.ZERO
    private var selectedContact: Contact? = null

    private var mPhoneContactUtils: PhoneContactUtils? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PaymentRequestActivity) {
            mListener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaymentSendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.paymentRequestRate.isVisible = Constant.SHOW_MOZO_EQUIVALENT_CURRENCY

        mPhoneContactUtils = PhoneContactUtils(binding.outputReceiverAddress) {
            selectedContact = it
            showContactInfoUI()
        }
        val myAddress = MozoWallet.getInstance().getAddress()
        myAddress ?: return

        val amount = arguments?.getString(KEY_AMOUNT)
        amount ?: return

        val content = "mozox:$myAddress?amount=$amount"
        generateQRJob = MozoSDK.scope.launch {
            val size = view.context.resources.dp2Px(177f).toInt()
            val qrImage = Support.generateQRCode(content, size)
            withContext(Dispatchers.Main) {
                binding.paymentRequestQrImage.setImageBitmap(qrImage)
            }
        }

        amountBigDecimal = amount.toBigDecimal()
        binding.paymentRequestAmount.text = amountBigDecimal.displayString()

        binding.outputReceiverAddress.apply {
            onTextChanged {
                hideErrorAddressUI()
                updateSubmitButton()
            }

            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                binding.outputReceiverAddressLabel.isSelected = hasFocus
                binding.outputReceiverAddressUnderline.isSelected = hasFocus
                if (hasFocus && binding.outputReceiverAddress.length() > 0)
                    binding.outputReceiverAddress.showDropDown()
            }

            setOnClickListener {
                if (binding.outputReceiverAddress.length() > 0)
                    binding.outputReceiverAddress.showDropDown()
            }

            threshold = 1
            setOnItemClickListener { _, _, position, _ ->
                (adapter.getItem(position) as? Contact)?.let {
                    selectedContact = it
                    showContactInfoUI()
                    binding.outputReceiverAddress.apply {
                        text = null
                        clearFocus()
                        hideKeyboard()
                    }
                }
            }
            setAdapter(ContactSuggestionAdapter(
                    context,
                    MozoSDK.getInstance().contactViewModel.contacts(),
                    mPhoneContactUtils?.onFindInSystemClick
            ))
        }

        binding.buttonScanQr.click {
            Support.scanQRCode(this@PaymentRequestSendFragment)
        }

        binding.buttonAddressBook.click {
            AddressBookActivity.startForResult(this, KEY_PICK_ADDRESS)
        }

        binding.buttonSend.click {
            if (!validateInput()) {
                showErrorAddressUI()
                return@click
            }

            mListener?.onSendRequestClicked(
                    amount,
                (selectedContact?.soloAddress
                                        ?: binding.outputReceiverAddress.text.toString()).lowercase(
                    Locale.getDefault()
                ),
                    PaymentRequest(content = content)
            )
        }

        MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.observe(
                viewLifecycleOwner,
                Observer {
                    it ?: return@Observer
                    binding.paymentRequestRate.text = MozoSDK.getInstance().profileViewModel
                            .formatCurrencyDisplay(
                                    amountBigDecimal.multiply(it.rate),
                                    true
                            )
                }
        )
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
            data != null -> {
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data).contents?.let {
                    if (WalletUtils.isValidAddress(it)) {
                        selectedContact = MozoSDK.getInstance().contactViewModel.findByAddress(it)

                        showInputUI()
                        if (selectedContact == null) {
                            binding.outputReceiverAddress.setText(it)
                            binding.outputReceiverAddress.dismissDropDown()
                            validateInput(true)
                        } else
                            showContactInfoUI()
                        updateSubmitButton()
                    } else if (context != null) {
                        MessageDialog.show(requireContext(), R.string.mozo_dialog_error_scan_invalid_msg)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        generateQRJob?.cancel()
    }

    override fun onDestroyView() {
        mPhoneContactUtils = null
        super.onDestroyView()
        _binding = null
    }

    private fun showContactInfoUI() {
        hideErrorAddressUI()
        updateSubmitButton()

        selectedContact?.run {
            binding.outputReceiverAddress.visibility = View.INVISIBLE
            binding.outputReceiverAddress.hideKeyboard()
            binding.outputReceiverAddressUnderline.visibility = View.INVISIBLE
            binding.buttonScanQr.visibility = View.INVISIBLE

            binding.outputReceiverAddressUser.visible()
            binding.outputReceiverIcon.setImageResource(if (isStore) R.drawable.ic_store else R.drawable.ic_receiver)
            binding.textReceiverPhone.text = if (isStore) physicalAddress else phoneNo
            binding.textReceiverPhone.isVisible = binding.textReceiverPhone.length() > 0

            binding.textReceiverUserName.text = name
            binding.textReceiverUserName.isVisible = !name.isNullOrEmpty()
            binding.textReceiverUserAddress.text = soloAddress

            binding.buttonClear.apply {
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
        visible(
                binding.outputReceiverAddress,
                binding.outputReceiverAddressUnderline,
                binding.buttonScanQr
        )
        binding.outputReceiverAddressUser.gone()
    }

    private fun updateSubmitButton() {
        binding.buttonSend.isEnabled = selectedContact != null || binding.outputReceiverAddress.length() > 0
    }

    @SuppressLint("SetTextI18n")
    private fun validateInput(fromScan: Boolean = false): Boolean {
        var isValidAddress = true
        val address = selectedContact?.soloAddress ?: binding.outputReceiverAddress.text.toString()
        if (!WalletUtils.isValidAddress(address)) {
            if (fromScan) binding.outputReceiverAddress.text = null
            showErrorAddressUI(fromScan)
            isValidAddress = false
        }

        if (fromScan) return isValidAddress

        if (!isValidAddress && context != null) mPhoneContactUtils?.validatePhone(requireContext(), address)?.let {
            if (it > 0) showErrorAddressUI(false, it)
        }

        return isValidAddress
    }

    private fun showErrorAddressUI(fromScan: Boolean = false, @StringRes errorId: Int = R.string.mozo_transfer_receiver_address_error) {
        val errorColor = context?.color(R.color.mozo_color_error) ?: return
        binding.outputReceiverAddressLabel.setTextColor(errorColor)
        binding.outputReceiverAddressUnderline.setBackgroundColor(errorColor)
        binding.outputReceiverAddressErrorMsg.visible()
        binding.outputReceiverAddressErrorMsg.setText(
                if (fromScan) R.string.mozo_dialog_error_scan_invalid_msg else errorId
        )
    }

    private fun hideErrorAddressUI() {
        binding.outputReceiverAddressLabel.setTextColor(
                ContextCompat.getColorStateList(context ?: return,
                        R.color.mozo_color_input_focus)
        )
        binding.outputReceiverAddressUnderline.setBackgroundResource(R.drawable.mozo_color_line_focus)
        binding.outputReceiverAddressErrorMsg.gone()
    }

    companion object {

        private const val KEY_PICK_ADDRESS = 0x0021
        private const val KEY_AMOUNT = "key_amount"

        fun getInstance(amount: String) = PaymentRequestSendFragment().apply {
            arguments = Bundle().apply { putString(KEY_AMOUNT, amount) }
        }
    }
}