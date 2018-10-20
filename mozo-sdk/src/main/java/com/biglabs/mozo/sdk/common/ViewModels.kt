package com.biglabs.mozo.sdk.common

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.biglabs.mozo.sdk.MozoSDK
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

        init {
            fetchData()
        }

        fun fetchData() = async {
            val profile = MozoDatabase.getInstance(MozoSDK.context!!).profile().getCurrentUserProfile()
            launch(UI) {
                profileLiveData.value = profile
                fetchBalance()
            }
        }

        fun fetchBalance() = async {
            profileLiveData.value?.walletInfo?.offchainAddress?.run {
                val balanceInfo = MozoService.getInstance(MozoSDK.context!!).getBalance(this).await()
                launch(UI) {
                    balanceInfoLiveData.value = balanceInfo
                    fetchExchangeRate()
                }
            }
        }

        fun fetchExchangeRate() = async {
            val rate = MozoService.getInstance(MozoSDK.context!!).getExchangeRate(Constant.CURRENCY_KOREA).await()
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

        init {
            fetchData()
        }

        fun fetchData() = async {
            val response = MozoService.getInstance(MozoSDK.context!!).getContacts { fetchData() }.await()
            response?.let { contactsResponse ->
                launch(UI) {
                    contactsLiveData.value = (contactsResponse.sortedBy { it.name })
                }
            }
        }

        fun findByAddress(address: String?) = contactsLiveData.value?.find { it.soloAddress.equals(address, ignoreCase = true) }
    }
}