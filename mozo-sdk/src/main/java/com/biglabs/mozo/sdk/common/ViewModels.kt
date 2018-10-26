package com.biglabs.mozo.sdk.common

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import com.biglabs.mozo.sdk.core.MozoDatabase
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.utils.displayString
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
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

        fun fetchData(context: Context) = async {
            val profile = MozoDatabase.getInstance(context).profile().getCurrentUserProfile()
            launch(UI) {
                profileLiveData.value = profile
                fetchBalance(context)
            }
        }

        fun fetchBalance(context: Context) = async {
            profileLiveData.value?.walletInfo?.offchainAddress?.run {
                val balanceInfo = MozoService.getInstance(context).getBalance(this) {
                    fetchBalance(context)
                }.await()
                launch(UI) {
                    balanceInfoLiveData.value = balanceInfo
                    fetchExchangeRate(context)
                }
            }
        }

        fun fetchExchangeRate(context: Context) = async {
            val rate = MozoService.getInstance(context).getExchangeRate(Constant.CURRENCY_KOREA) {
                fetchExchangeRate(context)
            }.await()
            launch(UI) {
                exchangeRateLiveData.value = rate
                updateBalanceAndRate()
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

        fun clear() = launch(UI) {
            profileLiveData.value = null
            balanceInfoLiveData.value = null
        }
    }

    class ContactViewModel : ViewModel() {
        val contactsLiveData = MutableLiveData<List<Models.Contact>>()

        fun fetchData(context: Context) = async {
            val response = MozoService.getInstance(context).getContacts { fetchData(context) }.await()
            response?.let { contactsResponse ->
                launch(UI) {
                    contactsLiveData.value = (contactsResponse.sortedBy { it.name })
                }
            }
        }

        fun findByAddress(address: String?) = contactsLiveData.value?.find { it.soloAddress.equals(address, ignoreCase = true) }
    }
}