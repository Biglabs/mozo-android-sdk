package io.mozocoin.sdk.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Px
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.R
import io.mozocoin.sdk.databinding.FragmentMozoWalletBinding
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.find
import io.mozocoin.sdk.wallet.OffChainWalletFragment
import io.mozocoin.sdk.wallet.OnChainWalletFragment

class MozoWalletFragment : Fragment() {

    private var _binding: FragmentMozoWalletBinding? = null
    private val binding get() = _binding!!
    private val tabFragments = arrayListOf<Fragment>(OffChainWalletFragment.getInstance(), OnChainWalletFragment.getInstance())
    private var unLoadedTabPosition = -1
    private var mPadding = arrayOf(/* left */0, /* top */0, /* right */0, /* bottom */0)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMozoWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rootContainer.updatePadding(mPadding[0], mPadding[1], mPadding[2], mPadding[3])

        binding.walletFragmentLoginRequired.findViewById<View>(R.id.button_login).click {
            MozoAuth.getInstance().signIn()
        }

        binding.walletFragmentTabs.apply {
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
        loadFragment(binding.walletFragmentTabs.selectedTabPosition)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) checkLogin()
    }

    override fun onResume() {
        super.onResume()
        checkLogin()
    }

    private fun loadFragment(position: Int) {
        if (!MozoAuth.getInstance().isSignedIn()) {
            unLoadedTabPosition = position
            return
        }

        val fragment = when (position) {
            1 -> tabFragments[1]
            else -> tabFragments[0]
        }

        childFragmentManager.beginTransaction().run {
            childFragmentManager.fragments.forEach {
                hide(it)
            }

            if (fragment.isAdded) show(fragment)
            else add(R.id.wallet_fragment_content, fragment)
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
            MozoAuth.getInstance().isSignUpCompleted(context ?: MozoSDK.getInstance().context) {
                if (unLoadedTabPosition > -1) loadFragment(unLoadedTabPosition)
                unLoadedTabPosition = -1
            }
        }
    }

    @Suppress("unused")
    fun updatePadding(@Px left: Int = 0, @Px top: Int = 0, @Px right: Int = 0, @Px bottom: Int = 0) {
        mPadding[0] = left
        mPadding[1] = top
        mPadding[2] = right
        mPadding[3] = bottom
        if (_binding != null) {
            binding.rootContainer.updatePadding(left, top, right, bottom)
        }
    }

    companion object {
        fun getInstance() = MozoWalletFragment()
    }
}