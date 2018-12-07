package com.biglabs.mozo.sdk.common

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.biglabs.mozo.sdk.core.MozoDatabase
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.utils.displayString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*

internal object ViewModels {

    data class BalanceAndRate(
            val balanceInDecimal: BigDecimal,
            val balanceInCurrency: BigDecimal,
            val balanceInCurrencyDisplay: String,
            val decimal: Int,
            val rate: BigDecimal
    )

    class ProfileViewModel : ViewModel() {

        var profileLiveData = MutableLiveData<Models.Profile?>()
        var balanceInfoLiveData = MutableLiveData<Models.BalanceInfo?>()
        var exchangeRateLiveData = MutableLiveData<Models.ExchangeRate?>()

        val balanceAndRateLiveData = MutableLiveData<BalanceAndRate>()

        fun fetchData(context: Context, callback: (() -> Unit)? = null) = GlobalScope.async {
            val profile = MozoDatabase.getInstance(context).profile().getCurrentUserProfile()
            launch(Dispatchers.Main) {
                profileLiveData.value = profile
                callback?.invoke()
                fetchBalance(context)
            }

            return@async profile
        }

        fun fetchBalance(context: Context) {
            GlobalScope.launch {
                profileLiveData.value?.walletInfo?.offchainAddress?.run {
                    val balanceInfo = MozoService.getInstance(context).getBalance(this) {
                        fetchBalance(context)
                    }.await()
                    launch(Dispatchers.Main) {
                        balanceInfoLiveData.value = balanceInfo
                        fetchExchangeRate(context)
                    }
                }
            }
        }

        fun fetchExchangeRate(context: Context) {
            GlobalScope.launch {
                val rate = MozoService.getInstance(context).getExchangeRate(Constant.CURRENCY_KOREA) {
                    fetchExchangeRate(context)
                }.await()
                launch(Dispatchers.Main) {
                    exchangeRateLiveData.value = rate
                    updateBalanceAndRate()
                }
            }
        }

        private fun updateBalanceAndRate() {
            val balanceInDecimal = balanceInfoLiveData.value?.balanceInDecimal() ?: BigDecimal.ZERO
            val rate = (exchangeRateLiveData.value?.rate ?: 0.0).toBigDecimal()
            val balanceInCurrency = balanceInDecimal.multiply(rate)
            balanceAndRateLiveData.value = BalanceAndRate(
                    balanceInDecimal,
                    balanceInCurrency,
                    String.format(Locale.US, "₩%s", balanceInCurrency.displayString()),
                    balanceInfoLiveData.value?.decimals ?: 0,
                    rate
            )
        }

        fun updateProfile(p: Models.Profile) = GlobalScope.launch(Dispatchers.Main) {
            profileLiveData.value = p
        }

        fun hasWallet() = profileLiveData.value?.walletInfo != null

        fun clear() = GlobalScope.launch(Dispatchers.Main) {
            profileLiveData.value = null
            balanceInfoLiveData.value = null
        }
    }

    class ContactViewModel : ViewModel() {
        val contactsLiveData = MutableLiveData<List<Models.Contact>>()

        fun fetchData(context: Context) {
            GlobalScope.launch {
                MozoService.getInstance(context).getContacts {
                    fetchData(context)
                }.await()?.let { contactsResponse ->
                    launch(Dispatchers.Main) {
                        contactsLiveData.value = (contactsResponse.sortedBy { it.name })
                    }
                }
            }
        }

        fun findByAddress(address: String?) = contactsLiveData.value?.find { it.soloAddress.equals(address, ignoreCase = true) }
    }
}