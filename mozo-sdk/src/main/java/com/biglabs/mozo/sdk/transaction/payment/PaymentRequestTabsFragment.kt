package com.biglabs.mozo.sdk.transaction.payment

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.biglabs.mozo.sdk.R
import com.biglabs.mozo.sdk.utils.hideKeyboard
import com.biglabs.mozo.sdk.utils.replace
import kotlinx.android.synthetic.main.fragment_payment_tabs.*

class PaymentRequestTabsFragment : Fragment() {

    private val tabFragments = arrayListOf(PaymentTabListFragment.getInstance(), PaymentTabCreateFragment.getInstance())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_payment_tabs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        payment_tabs.setOnCheckedChangeListener { group, checkedId ->
            group.hideKeyboard()
            loadFragment(checkedId)
        }

        payment_tab_list.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.typeface = if (isChecked) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        payment_tab_create.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.typeface = if (isChecked) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        loadFragment(R.id.payment_tab_list)
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