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
import io.mozocoin.sdk.utils.safe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.*

internal object ViewModels {

    data class BalanceAndRate(
            val balanceNonDecimal: BigDecimal,
            val balanceNonDecimalInCurrency: BigDecimal,
            val balanceNonDecimalInCurrencyDisplay: String,
            val decimal: Int,
            val rate: BigDecimal,
            val realValues: Boolean = true
    )

    class ProfileViewModel : ViewModel() {

        var userInfoLiveData = MutableLiveData<UserInfo?>()
        var profileLiveData = MutableLiveData<Profile?>()
        var balanceInfoLiveData = MutableLiveData<BalanceInfo?>()
        var exchangeRateLiveData = MutableLiveData<ExchangeRateData?>()

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
                MozoAPIsService.getInstance().getBalance(context, address, { data, _ ->
                    callback?.invoke(data)
                    data ?: return@getBalance
                    balanceInfoLiveData.value = data
                    fetchExchangeRate(context)
                }, {
                    fetchBalance(context, keepTry, callback)
                })
            }
        }

        fun fetchExchangeRate(context: Context) {
            updateBalanceAndRate()

            MozoAPIsService.getInstance().getExchangeRate(context, Locale.getDefault().language, { data, _ ->
                if (data != null) {
                    exchangeRateLiveData.value = data
                    if (data.token?.currency == Constant.DEFAULT_CURRENCY) {
                        SharedPrefsUtils.setDefaultCurrencyRate(data.token.rate())
                    }
                } else {
                    exchangeRateLiveData.value = Support.getDefaultCurrency()
                }

                updateBalanceAndRate()
            }, {
                fetchExchangeRate(context)
            })
        }

        private fun updateBalanceAndRate() = MainScope().launch {
            exchangeRateLiveData.value = exchangeRateLiveData.value ?: Support.getDefaultCurrency()
            val balanceNonDecimal = balanceInfoLiveData.value?.balanceNonDecimal().safe()
            val rate = exchangeRateLiveData.value?.token?.rate().safe()
            val balanceInCurrency = balanceNonDecimal.multiply(rate)

            balanceAndRateLiveData.value = BalanceAndRate(
                    balanceNonDecimal,
                    balanceInCurrency,
                    formatCurrencyDisplay(balanceInCurrency),
                    balanceInfoLiveData.value?.decimals
                            ?: Constant.DEFAULT_DECIMAL,
                    rate,
                    realValues = balanceInfoLiveData.value != null
            )
        }

        fun updateProfile(context: Context, p: Profile) = GlobalScope.launch(Dispatchers.Main) {
            profileLiveData.value = p
            fetchBalance(context)
        }

        fun updateUserInfo(u: UserInfo) = GlobalScope.launch(Dispatchers.Main) {
            userInfoLiveData.value = u
            MozoAuth.invokeProfileChangeListener(u)
        }

        fun hasWallet() = profileLiveData.value?.walletInfo != null

        fun getProfile() = profileLiveData.value

        fun getBalance() = balanceInfoLiveData.value

        fun getBalanceInCurrencyDisplay() = balanceAndRateLiveData.value?.balanceNonDecimalInCurrencyDisplay

        fun formatCurrencyDisplay(amount: BigDecimal, withBracket: Boolean = false) = StringBuilder().apply {
            val symbol = exchangeRateLiveData.value?.token?.currencySymbol
                    ?: Constant.DEFAULT_CURRENCY_SYMBOL
            val finalAmount = if (symbol == Constant.CURRENCY_SYMBOL_VND) {
                val raw = amount.setScale(0, BigDecimal.ROUND_UP)
                val rounded = raw.round(MathContext(
                        (raw.precision() - 3).coerceAtLeast(0),
                        RoundingMode.HALF_EVEN
                ))
                when {
                    rounded < BigDecimal.valueOf(500) -> BigDecimal.ZERO
                    rounded < BigDecimal.valueOf(1000) -> BigDecimal.valueOf(1000)
                    else -> rounded
                }
            } else amount

            if (withBracket) append("(")
            append(symbol)
            append(finalAmount.displayString(3))
            if (withBracket) append(")")
        }.toString()

        fun calculateAmountInCurrency(amount: BigDecimal, useOffChain: Boolean = true) = formatCurrencyDisplay(
                amount.multiply((if (useOffChain) balanceAndRateLiveData.value?.rate else exchangeRateLiveData.value?.eth?.rate).safe())
        )

        fun clear() = GlobalScope.launch(Dispatchers.Main) {
            profileLiveData.value = null
            balanceInfoLiveData.value = null
        }
    }

    class ContactViewModel : ViewModel() {
        private val countriesLiveData = MutableLiveData<List<CountryCode>>()
        val usersLiveData = MutableLiveData<List<Contact>>()
        private val storesLiveData = MutableLiveData<List<Contact>>()

        fun fetchCountries(context: Context, callback: (() -> Unit)? = null) {
            MozoAPIsService.getInstance().getCountries(context, { data, _ ->
                if (data?.items != null) {
                    countriesLiveData.value = data.items
                }
                callback?.invoke()

            }, {
                fetchCountries(context, callback)
            })
        }

        fun fetchUser(context: Context, callback: (() -> Unit)? = null) {
            MozoAPIsService.getInstance().getContactUsers(context, { data, _ ->
                if (data?.items != null) {
                    usersLiveData.value = data.items!!.sortedBy { it.name }
                }
                callback?.invoke()
            }, {
                fetchUser(context, callback)
            })
        }

        fun fetchStore(context: Context, callback: (() -> Unit)? = null) {
            MozoAPIsService.getInstance().getContactStores(context, { data, _ ->
                if (data?.items != null) {
                    storesLiveData.value = data.items!!.sortedBy { it.apply { isStore = true }.name }
                }
                callback?.invoke()
            }, {
                fetchStore(context, callback)
            })
        }

        fun fetchData(context: Context) {
            fetchCountries(context)
            fetchUser(context)
            fetchStore(context)
        }

        fun findByAddress(address: String?) = usersLiveData.value?.find {
            it.soloAddress.equals(address, ignoreCase = true)
        } ?: storesLiveData.value?.find {
            it.soloAddress.equals(address, ignoreCase = true)
        }

        fun find(keyword: String) = usersLiveData.value?.filter {
            it.name?.contains(keyword, ignoreCase = true) == true
                    || it.phoneNo?.contains(keyword, ignoreCase = true) == true
                    || it.soloAddress?.contains(keyword, ignoreCase = true) == true

        } ?: storesLiveData.value?.filter {
            it.name?.contains(keyword, ignoreCase = true) == true
                    || it.phoneNo?.contains(keyword, ignoreCase = true) == true
                    || it.soloAddress?.contains(keyword, ignoreCase = true) == true
        }

        fun containCountryCode(code: String): Boolean = countriesLiveData.value?.find {
            code.startsWith(it.countryCode ?: "")
        } != null

        fun users(): List<Contact> = usersLiveData.value ?: emptyList()
        fun stores(): List<Contact> = storesLiveData.value ?: emptyList()
        fun contacts(): List<Contact> = users() + stores()
    }
}