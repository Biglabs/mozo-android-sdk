package io.mozocoin.sdk.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.find
import kotlinx.android.synthetic.main.fragment_mozo_wallet.*
import kotlinx.android.synthetic.main.view_wallet_state_not_login.*

class MozoWalletFragment : Fragment() {

    private val tabFragments = arrayListOf(MozoWalletOffChainFragment.getInstance(), MozoWalletOnChainFragment.getInstance())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_mozo_wallet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_login?.click {
            MozoAuth.getInstance().signIn()
        }

        wallet_fragment_tabs?.apply {
            for (i in 1..tabCount) {
                updateTextStyle(getTabAt(i), false)
            }
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(p0: TabLayout.Tab?) {
                }

                override fun onTabUnselected(p0: TabLayout.Tab?) {
                    updateTextStyle(p0, false)
                }

                override fun onTabSelected(p0: TabLayout.Tab?) {
                    updateTextStyle(p0, true)
                    loadFragment(p0?.position ?: 0)
                }
            })
        }
        loadFragment(wallet_fragment_tabs.selectedTabPosition)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) checkLogin()
    }

    override fun onResume() {
        super.onResume()
        checkLogin()
    }

    private fun loadFragment(position: Int) {
        val fragment = when (position) {
            1 -> tabFragments[1]
            else -> tabFragments[0]
        }

        childFragmentManager.beginTransaction().run {
            childFragmentManager.fragments.forEach {
                hide(it)
            }

            if (fragment.isAdded) show(fragment)
            else add(R.id.wallet_fragment_contain, fragment)
        }.commit()
    }

    private fun updateTextStyle(tab: TabLayout.Tab?, selected: Boolean) {
        @Suppress("INACCESSIBLE_TYPE")
        tab?.view?.children?.forEach { v ->
            (v as? TextView)?.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private fun checkLogin() {
        view?.find<View>(R.id.wallet_fragment_login_required)?.apply {
            isClickable = true
            isVisible = !MozoAuth.getInstance().isSignedIn()
        }

        if (MozoAuth.getInstance().isSignedIn()) {
            MozoAuth.getInstance().isSignUpCompleted {
                /* no need to handle here */
            }
        }
    }

    companion object {
        fun getInstance() = MozoWalletFragment()
    }
}