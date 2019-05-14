package io.mozocoin.sdk.wallet.reset

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.mozocoin.sdk.common.WalletHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class ResetPinViewModel : ViewModel() {
    private var walletLiveData = MutableLiveData<WalletHelper?>()

    fun getData() = walletLiveData.value

    fun setData(walletHelper: WalletHelper?) = GlobalScope.launch(Dispatchers.Main) {
        walletLiveData.value = walletHelper
    }

    companion object {
        fun get(owner: AppCompatActivity) = ViewModelProvider(owner, ViewModelProvider.NewInstanceFactory())
                .get(ResetPinViewModel::class.java)
    }
}