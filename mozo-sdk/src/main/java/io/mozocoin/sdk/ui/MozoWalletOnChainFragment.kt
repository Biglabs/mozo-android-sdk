package io.mozocoin.sdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.mozocoin.sdk.R

class MozoWalletOnChainFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_mozo_wallet_on, container, false)

    companion object {
        fun getInstance() = MozoWalletOnChainFragment()
    }
}