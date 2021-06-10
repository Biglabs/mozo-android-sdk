package io.mozocoin.sdk.transaction.payment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.mozocoin.sdk.MozoWallet
import io.mozocoin.sdk.common.Constant
import io.mozocoin.sdk.databinding.FragmentPaymentCreateBinding
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.onAmountInputChanged
import java.math.BigDecimal

class PaymentTabCreateFragment : Fragment() {

    private var _binding: FragmentPaymentCreateBinding? = null
    private val binding get() = _binding!!
    private var mListener: PaymentRequestInteractionListener? = null
    private var mInputAmount = BigDecimal.ZERO

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PaymentRequestActivity) {
            mListener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaymentCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.outputAmount.onAmountInputChanged(
            textChanged = {
                if (it.isNullOrEmpty()) {
                    binding.outputAmountRate.text = ""
                    return@onAmountInputChanged
                }

            },
            amountChanged = {
                mInputAmount = it
                binding.outputAmountRate.text = MozoWallet.getInstance().amountInCurrency(mInputAmount)

                updateSubmitButton()
            }
        )

        binding.outputAmountRate.isVisible = Constant.SHOW_MOZO_EQUIVALENT_CURRENCY

        binding.buttonSubmit.click {
            mListener?.onCreateRequestClicked(mInputAmount.toString())
        }
    }

    private fun updateSubmitButton() {
        binding.buttonSubmit.isEnabled = binding.outputAmount.length() > 0 && mInputAmount > BigDecimal.ZERO
    }

    companion object {

        fun getInstance() = PaymentTabCreateFragment()
    }
}