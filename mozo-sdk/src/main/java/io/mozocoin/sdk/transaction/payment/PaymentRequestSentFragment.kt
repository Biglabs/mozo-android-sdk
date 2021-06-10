package io.mozocoin.sdk.transaction.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.databinding.FragmentPaymentSentBinding
import io.mozocoin.sdk.utils.displayString
import io.mozocoin.sdk.utils.gone
import io.mozocoin.sdk.utils.safe
import io.mozocoin.sdk.utils.visible

class PaymentRequestSentFragment : Fragment() {
    private var _binding: FragmentPaymentSentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaymentSentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.paymentRequestRate.isVisible = Constant.SHOW_MOZO_EQUIVALENT_CURRENCY

        /*payment_request_address.text = MozoSDK.getInstance()
                .contactViewModel
                .findByAddress(address)?.name ?: address
*/
        updateContactUI()

        val amount = arguments?.getString(KEY_AMOUNT)?.toBigDecimal().safe()
        binding.paymentRequestAmount.text = amount.displayString()

        MozoSDK.getInstance().profileViewModel.balanceAndRateLiveData.observe(viewLifecycleOwner) {
            binding.paymentRequestRate.text = MozoSDK.getInstance().profileViewModel
                    .formatCurrencyDisplay(
                            amount.multiply(it.rate),
                            true
                    )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateContactUI() {
        val address = arguments?.getString(KEY_ADDRESS)
        val contact = MozoSDK.getInstance().contactViewModel.findByAddress(address)
        if (contact == null) {
            binding.outputReceiverAddressUser.gone()
            binding.paymentRequestAddress.text = address
            return
        }

        binding.outputReceiverAddressUser.visible()
        binding.outputReceiverIcon.setImageResource(if (contact.isStore) R.drawable.ic_store else R.drawable.ic_receiver)
        binding.textReceiverPhone.text = if (contact.isStore) contact.physicalAddress else contact.phoneNo
        binding.textReceiverPhone.isVisible = binding.textReceiverPhone.length() > 0

        binding.textReceiverUserName.text = contact.name
        binding.textReceiverUserName.isVisible = !contact.name.isNullOrEmpty()
        binding.textReceiverUserAddress.text = contact.soloAddress
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