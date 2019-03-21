package io.mozocoin.sdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.R
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.find
import kotlinx.android.synthetic.main.view_wallet_state_not_login.*

class MozoWalletFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_mozo_wallet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_login?.click {
            MozoAuth.getInstance().signIn()
        }

        childFragmentManager.beginTransaction()
                .replace(R.id.wallet_fragment_contain, MozoWalletOffChainFragment.getInstance())
                .commit()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) checkLogin()
    }

    override fun onResume() {
        super.onResume()
        checkLogin()
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