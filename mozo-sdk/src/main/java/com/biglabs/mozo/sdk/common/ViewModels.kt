package com.biglabs.mozo.sdk.common

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.biglabs.mozo.sdk.common.model.BalanceInfo
import com.biglabs.mozo.sdk.common.model.Contact
import com.biglabs.mozo.sdk.common.model.ExchangeRate
import com.biglabs.mozo.sdk.common.model.Profile
import com.biglabs.mozo.sdk.core.MozoDatabase
import com.biglabs.mozo.sdk.core.MozoService
import com.biglabs.mozo.sdk.utils.displayString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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

        var profileLiveData = MutableLiveData<Profile?>()
        var balanceInfoLiveData = MutableLiveData<BalanceInfo?>()
        var exchangeRateLiveData = MutableLiveData<ExchangeRate?>()

        val balanceAndRateLiveData = MutableLiveData<BalanceAndRate>()

        fun fetchData(context: Context, callback: ((p: Profile?) -> Unit)? = null) {
            GlobalScope.launch {
                val profile = MozoDatabase.getInstance(context).profile().getCurrentUserProfile()
                launch(Dispatchers.Main) {
                    profileLiveData.value = profile
                    callback?.invoke(profile)
                    fetchBalance(context)
                }
            }
        }

        fun fetchBalance(context: Context, callback: ((balanceInfo: BalanceInfo?) -> Unit)? = null) {
            profileLiveData.value?.walletInfo?.offchainAddress?.run {
                MozoService.getInstance().getBalance(context, this) { data, _ ->
                    data ?: return@getBalance
                    balanceInfoLiveData.value = data
                    callback?.invoke(data)
                    fetchExchangeRate(context)
                }
            }
        }

        fun fetchExchangeRate(context: Context) {
            MozoService.getInstance().getExchangeRate(context, Constant.CURRENCY_KOREA, Constant.SYMBOL_MOZO) { data, _ ->
                data ?: return@getExchangeRate
                exchangeRateLiveData.value = data
                updateBalanceAndRate()
            }
        }

        fun getBalance() = balanceInfoLiveData.value

        private fun updateBalanceAndRate() {
            val balanceNonDecimal = balanceInfoLiveData.value?.balanceNonDecimal()
                    ?: BigDecimal.ZERO
            val rate = (exchangeRateLiveData.value?.rate ?: 0.0).toBigDecimal()
            val balanceInCurrency = balanceNonDecimal.multiply(rate)
            balanceAndRateLiveData.value = BalanceAndRate(
                    balanceNonDecimal,
                    balanceInCurrency,
                    String.format(Locale.US, "₩%s", balanceInCurrency.displayString()),
                    balanceInfoLiveData.value?.decimals ?: 0,
                    rate
            )
        }

        fun updateProfile(context: Context, p: Profile) = GlobalScope.launch(Dispatchers.Main) {
            profileLiveData.value = p
            fetchBalance(context)
        }

        fun hasWallet() = profileLiveData.value?.walletInfo != null

        fun clear() = GlobalScope.launch(Dispatchers.Main) {
            profileLiveData.value = null
            balanceInfoLiveData.value = null
        }
    }

    class ContactViewModel : ViewModel() {
        val usersLiveData = MutableLiveData<List<Contact>>()
        private val storesLiveData = MutableLiveData<List<Contact>>()

        fun fetchUser(context: Context, callback: (() -> Unit)? = null) {
            MozoService.getInstance().getContactUsers(context) { data, _ ->
                if (data?.items != null) {
                    usersLiveData.value = data.items!!.sortedBy { it.name }
                }
                callback?.invoke()
            }
        }

        fun fetchStore(context: Context, callback: (() -> Unit)? = null) {
            MozoService.getInstance().getContactStores(context) { data, _ ->
                if (data?.items != null) {
                    storesLiveData.value = data.items!!.sortedBy { it.apply { isStore = true }.name }
                }
                callback?.invoke()
            }
        }

        fun fetchData(context: Context) {
            fetchUser(context)
            fetchStore(context)
        }

        fun findByAddress(address: String?) = usersLiveData.value?.find {
            it.soloAddress.equals(address, ignoreCase = true)
        } ?: storesLiveData.value?.find {
            it.soloAddress.equals(address, ignoreCase = true)
        }

        fun users(): List<Contact> = usersLiveData.value ?: emptyList()
        fun stores(): List<Contact> = storesLiveData.value ?: emptyList()
    }
}