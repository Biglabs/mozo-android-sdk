package com.biglabs.mozo.sdk.transaction.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.biglabs.mozo.sdk.MozoSDK
import com.biglabs.mozo.sdk.MozoWallet
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.common.Models
import com.biglabs.mozo.sdk.common.ViewModels
import com.biglabs.mozo.sdk.contact.AddressBookActivity
import com.biglabs.mozo.sdk.utils.*
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.fragment_payment_send.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*

class PaymentRequestSendFragment : Fragment() {

    private var mListener: PaymentRequestInteractionListener? = null
    private var generateQRJob: Job? = null
    private var amountBigDecimal = BigDecimal.ZERO
    private var selectedContact: Models.Contact? = null

    override fun onAttach(context: Context?) {
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
            launch(Dispatchers.Main) {
                payment_request_qr_image.setImageBitmap(qrImage)
            }
        }

        amountBigDecimal = amount.toBigDecimal()
        payment_request_amount.text = amountBigDecimal.displayString()

        button_scan_qr.click {
            Support.scanQRCode(this@PaymentRequestSendFragment)
        }

        button_address_book.click {
            AddressBookActivity.startForResult(this, KEY_PICK_ADDRESS)
        }

        button_send.click {
            mListener?.onSendRequestClicked(
                    amount,
                    Models.PaymentRequest(
                            toAddress = selectedContact?.soloAddress
                                    ?: output_receiver_address.text.toString(),
                            content = content
                    )
            )
        }

        MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.observe(
                this,
                Observer<ViewModels.BalanceAndRate?> {
                    it ?: return@Observer
                    payment_request_rate.text = String.format(Locale.US, "(₩%s)", amountBigDecimal.multiply(it.rate).displayString())
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
                    selectedContact = MozoSDK.getInstance().contactViewModel.findByAddress(it)

                    showInputUI()
                    if (selectedContact == null) {
                        output_receiver_address.setText(it)
                    } else
                        showContactInfoUI()
                    updateSubmitButton()
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