package io.mozocoin.sdk.transaction.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.displayString
import io.mozocoin.sdk.utils.gone
import io.mozocoin.sdk.utils.safe
import io.mozocoin.sdk.utils.visible
import kotlinx.android.synthetic.main.fragment_payment_sent.*

class PaymentRequestSentFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_payment_sent, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*payment_request_address.text = MozoSDK.getInstance()
                .contactViewModel
                .findByAddress(address)?.name ?: address
*/
        updateContactUI()

        val amount = arguments?.getString(KEY_AMOUNT)?.toBigDecimal().safe()
        payment_request_amount.text = amount.displayString()

        MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.observe(viewLifecycleOwner, Observer {
            payment_request_rate.text = MozoSDK.getInstance().profileViewModel
                    .formatCurrencyDisplay(
                            amount.multiply(it.rate),
                            true
                    )
        })
    }

    private fun updateContactUI() {
        val address = arguments?.getString(KEY_ADDRESS)
        val contact = MozoSDK.getInstance().contactViewModel.findByAddress(address)
        if (contact == null) {
            output_receiver_address_user?.gone()
            payment_request_address.text = address
            return
        }

        output_receiver_address_user?.visible()
        output_receiver_icon?.setImageResource(if (contact.isStore) R.drawable.ic_store else R.drawable.ic_receiver)
        text_receiver_phone?.text = if (contact.isStore) contact.physicalAddress else contact.phoneNo
        text_receiver_phone?.isVisible = text_receiver_phone?.length() ?: 0 > 0

        text_receiver_user_name?.text = contact.name
        text_receiver_user_name?.isVisible = !contact.name.isNullOrEmpty()
        text_receiver_user_address?.text = contact.soloAddress
    }

    companion object {
        private const val KEY_AMOUNT = "key_amount"
        private const val KEY_ADDRESS = "key_address"

        fun getInstance(amount: String, toAddress: String) = PaymentRequestSentFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_AMOUNT, amount)
                putString(KEY_ADDRESS, toAddress)
            }
        }
    }
}