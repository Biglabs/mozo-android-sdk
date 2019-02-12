package io.mozocoin.sdk.common

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.mozocoin.sdk.common.model.BalanceInfo
import io.mozocoin.sdk.common.model.Contact
import io.mozocoin.sdk.common.model.ExchangeRate
import io.mozocoin.sdk.common.model.Profile
import io.mozocoin.sdk.common.service.MozoDatabase
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.utils.SharedPrefsUtils
import io.mozocoin.sdk.utils.Support
import io.mozocoin.sdk.utils.displayString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        fun fetchData(context: Context, userId: String? = null, callback: ((p: Profile?) -> Unit)? = null) {
            GlobalScope.launch {
                val profile = if (userId != null) MozoDatabase.getInstance(context).profile().get(userId)
                else MozoDatabase.getInstance(context).profile().getCurrentUserProfile()

                withContext(Dispatchers.Main) {
                    profileLiveData.value = profile
                    callback?.invoke(profile)
                    fetchBalance(context)
                }
            }
        }

        fun fetchBalance(context: Context, callback: ((balanceInfo: BalanceInfo?) -> Unit)? = null) {
            profileLiveData.value?.walletInfo?.offchainAddress?.run {
                MozoAPIsService.getInstance().getBalance(context, this) { data, _ ->
                    callback?.invoke(data)
                    data ?: return@getBalance
                    balanceInfoLiveData.value = data
                    fetchExchangeRate(context)
                }
            }
        }

        fun fetchExchangeRate(context: Context) {
            exchangeRateLiveData.value = Support.getDefaultCurrency()
            updateBalanceAndRate()

            MozoAPIsService.getInstance().getExchangeRate(context, Locale.getDefault().language) { data, _ ->
                if (data != null) {
                    exchangeRateLiveData.value = data
                    if (data.currency == Constant.DEFAULT_CURRENCY) {
                        SharedPrefsUtils.setDefaultCurrencyRate(data.rate)
                    }
                } else {
                    exchangeRateLiveData.value = Support.getDefaultCurrency()
                }

                updateBalanceAndRate()
            }
        }

        fun getProfile() = profileLiveData.value

        fun getBalance() = balanceInfoLiveData.value

        private fun updateBalanceAndRate() {
            val balanceNonDecimal = balanceInfoLiveData.value?.balanceNonDecimal()
                    ?: BigDecimal.ZERO
            val rate = (exchangeRateLiveData.value?.rate ?: 0.0).toBigDecimal()
            val balanceInCurrency = balanceNonDecimal.multiply(rate)

            balanceAndRateLiveData.value = BalanceAndRate(
                    balanceNonDecimal,
                    balanceInCurrency,
                    formatCurrencyDisplay(balanceInCurrency),
                    balanceInfoLiveData.value?.decimals
                            ?: Constant.DEFAULT_DECIMAL,
                    rate
            )
        }

        fun formatCurrencyDisplay(amount: BigDecimal, withBracket: Boolean = false) = StringBuilder().apply {
            if (withBracket) append("(")
            append(exchangeRateLiveData.value?.currencySymbol ?: Constant.DEFAULT_CURRENCY_SYMBOL)
            append(amount.displayString())
            if (withBracket) append(")")
        }.toString()

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
            MozoAPIsService.getInstance().getContactUsers(context) { data, _ ->
                if (data?.items != null) {
                    usersLiveData.value = data.items!!.sortedBy { it.name }
                }
                callback?.invoke()
            }
        }

        fun fetchStore(context: Context, callback: (() -> Unit)? = null) {
            MozoAPIsService.getInstance().getContactStores(context) { data, _ ->
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