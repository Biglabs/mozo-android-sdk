package io.mozocoin.sdk.common

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.mozocoin.sdk.MozoAuth
import io.mozocoin.sdk.common.model.*
import io.mozocoin.sdk.common.service.MozoAPIsService
import io.mozocoin.sdk.common.service.MozoDatabase
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

        var userInfoLiveData = MutableLiveData<UserInfo?>()
        var profileLiveData = MutableLiveData<Profile?>()
        var balanceInfoLiveData = MutableLiveData<BalanceInfo?>()
        var exchangeRateLiveData = MutableLiveData<ExchangeRate?>()

        val balanceAndRateLiveData = MutableLiveData<BalanceAndRate>()

        init {
            updateBalanceAndRate()
        }

        @Synchronized
        fun fetchData(context: Context, userId: String? = null, callback: ((p: Profile?) -> Unit)? = null) {
            if (!MozoAuth.getInstance().isSignedIn()) {
                callback?.invoke(null)
                return
            }
            GlobalScope.launch {
                val profile = if (userId != null) MozoDatabase.getInstance(context).profile().get(userId)
                else MozoDatabase.getInstance(context).profile().getCurrentUserProfile()

                val userInfo = MozoDatabase.getInstance(context).userInfo().get()

                withContext(Dispatchers.Main) {
                    userInfoLiveData.value = userInfo
                    profileLiveData.value = profile
                    callback?.invoke(profile)
                    fetchBalance(context, false)
                }
            }
        }

        fun fetchBalance(context: Context, keepTry: Boolean = true, callback: ((balanceInfo: BalanceInfo?) -> Unit)? = null) {
            if (!MozoAuth.getInstance().isSignedIn()) {
                callback?.invoke(null)
                return
            }
            val address = profileLiveData.value?.walletInfo?.offchainAddress
            if (address == null) {
                if (keepTry) fetchData(context, callback = {
                    if (it == null) callback?.invoke(null)
                    fetchBalance(context, false, callback)
                })
                else callback?.invoke(null)
            } else {
                MozoAPIsService.getInstance().getBalance(context, address) { data, _ ->
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

        private fun updateBalanceAndRate() {
            if (exchangeRateLiveData.value == null) {
                exchangeRateLiveData.value = Support.getDefaultCurrency()
            }
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

        fun updateProfile(context: Context, p: Profile) = GlobalScope.launch(Dispatchers.Main) {
            profileLiveData.value = p
            fetchBalance(context)
        }

        fun updateUserInfo(u: UserInfo) = GlobalScope.launch(Dispatchers.Main) {
            userInfoLiveData.value = u
        }

        fun hasWallet() = profileLiveData.value?.walletInfo != null

        fun getProfile() = profileLiveData.value

        fun getBalance() = balanceInfoLiveData.value

        fun getBalanceInCurrencyDisplay() = balanceAndRateLiveData.value?.balanceInCurrencyDisplay

        fun formatCurrencyDisplay(amount: BigDecimal, withBracket: Boolean = false) = StringBuilder().apply {
            if (withBracket) append("(")
            append(exchangeRateLiveData.value?.currencySymbol ?: Constant.DEFAULT_CURRENCY_SYMBOL)
            append(amount.displayString())
            if (withBracket) append(")")
        }.toString()

        fun calculateAmountInCurrency(amount: BigDecimal) = formatCurrencyDisplay(
                amount.multiply(balanceAndRateLiveData.value?.rate ?: BigDecimal.ZERO)
        )

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