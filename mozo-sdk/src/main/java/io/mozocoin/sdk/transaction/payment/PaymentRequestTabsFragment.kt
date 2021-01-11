package io.mozocoin.sdk.transaction.payment

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.mozocoin.sdk.R
import io.mozocoin.sdk.databinding.FragmentPaymentTabsBinding
import io.mozocoin.sdk.utils.hideKeyboard
import io.mozocoin.sdk.utils.replace

class PaymentRequestTabsFragment : Fragment() {
    private var _binding: FragmentPaymentTabsBinding? = null
    private val binding get() = _binding!!
    private val tabFragments = arrayListOf(PaymentTabListFragment.getInstance(), PaymentTabCreateFragment.getInstance())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaymentTabsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.paymentTabs.setOnCheckedChangeListener { group, checkedId ->
            group.hideKeyboard()
            loadFragment(checkedId)
        }

        binding.paymentTabList.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.typeface = if (isChecked) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        binding.paymentTabCreate.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.typeface = if (isChecked) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        loadFragment(R.id.payment_tab_list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadFragment(checkId: Int) {

        val fragment = when (checkId) {
            R.id.payment_tab_list -> tabFragments[0]
            else -> tabFragments[1]
        }

        replace(R.id.payment_tabs_content_frame, fragment)
    }

    companion object {

        fun getInstance() = PaymentRequestTabsFragment()
    }
}