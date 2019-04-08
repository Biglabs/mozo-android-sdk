package io.mozocoin.sdk.transaction.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.ViewModels
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.common.model.PaymentRequest
import io.mozocoin.sdk.contact.AddressBookActivity
import io.mozocoin.sdk.ui.dialog.MessageDialog
import com.google.zxing.integration.android.IntentIntegrator
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

        button_scan_qr.click {
            Support.scanQRCode(this@PaymentRequestSendFragment)
        }

        button_address_book.click {
            AddressBookActivity.startForResult(this, KEY_PICK_ADDRESS)
        }

        button_send.click {
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
                            output_receiver_address.setText(it)
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
        selectedContact ?: return

        output_receiver_address.setText(selectedContact?.soloAddress)
    }

    private fun showInputUI() {
        output_receiver_address.visible()
    }

    private fun updateSubmitButton() {
        button_send.isEnabled = selectedContact != null || output_receiver_address.length() > 0
    }

    companion object {

        private const val KEY_PICK_ADDRESS = 0x0021
        private const val KEY_AMOUNT = "key_amount"

        fun getInstance(amount: String) = PaymentRequestSendFragment().apply {
            arguments = Bundle().apply { putString(KEY_AMOUNT, amount) }
        }
    }
}