package io.mozocoin.sdk.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.common.Constant
import java.math.BigDecimal
import java.util.*

internal class SharedPrefsUtils private constructor() {
    companion object {
        private const val KEY_CURRENCY_RATE = "KEY_CURRENCY_RATE"
        private const val KEY_LANGUAGE = "KEY_LANGUAGE"
        private const val KEY_COUNTRY = "KEY_COUNTRY"
        private const val KEY_SHOW_AUTO_PIN_NOTICE = "KEY_SHOW_AUTO_PIN_NOTICE"

        private const val KEY_CONVERT_ON_IN_OFF_ADDRESS_LAST_TX =
            "KEY_CONVERT_ON_IN_OFF_ADDRESS_LAST_TX"
        private const val KEY_CONVERT_ON_IN_OFF_ADDRESS_AMOUNT =
            "KEY_CONVERT_ON_IN_OFF_ADDRESS_AMOUNT"

        private var instance: SharedPreferences? = null

        @JvmStatic
        internal fun manager(ctx: Context? = null): SharedPreferences {
            if (instance == null) {
                instance = PreferenceManager.getDefaultSharedPreferences(
                    ctx ?: MozoSDK.getInstance().context
                )
            }
            return instance!!
        }

        @JvmStatic
        fun getDefaultCurrencyRate(): BigDecimal {
            val rate = manager().getString(KEY_CURRENCY_RATE, "")
            return if (rate.isNullOrEmpty()) Constant.DEFAULT_CURRENCY_RATE.toBigDecimal()
            else rate.toBigDecimal()
        }

        @JvmStatic
        fun setDefaultCurrencyRate(rate: BigDecimal) {
            val rateStr = if (rate <= BigDecimal.ZERO) Constant.DEFAULT_CURRENCY_RATE.toString()
            else rate.toString()
            manager().edit().putString(KEY_CURRENCY_RATE, rateStr).apply()
        }

        @JvmStatic
        fun setLastInfoConvertOnChainInOffChain(txHash: String?, amount: String?) {
            manager().edit()
                .putString(KEY_CONVERT_ON_IN_OFF_ADDRESS_LAST_TX, txHash)
                .putString(KEY_CONVERT_ON_IN_OFF_ADDRESS_AMOUNT, amount)
                .apply()
        }

        @JvmStatic
        fun getLastTxConvertOnChainInOffChain(): String? {
            return manager().getString(KEY_CONVERT_ON_IN_OFF_ADDRESS_LAST_TX, null)
        }

        @JvmStatic
        fun getLastAmountConvertOnChainInOffChain(): String? {
            return manager().getString(KEY_CONVERT_ON_IN_OFF_ADDRESS_AMOUNT, null)
        }

        @JvmStatic
        var showAutoPinNotice: Boolean = true
            get() = manager().getBoolean(KEY_SHOW_AUTO_PIN_NOTICE, true)
            set(value) {
                field = value
                manager().edit().putBoolean(KEY_SHOW_AUTO_PIN_NOTICE, value).apply()
            }

        @JvmStatic
        var language: Locale = Locale.getDefault()
            get() {
                val default = Locale.getDefault()
                val language = manager().getString(KEY_LANGUAGE, default.language)
                val country = manager().getString(KEY_COUNTRY, default.country)
                return if (!language.isNullOrEmpty() && !country.isNullOrEmpty())
                    Locale(language, country) else default
            }
            set(value) {
                field = value
                manager().edit()
                    .putString(KEY_LANGUAGE, value.language)
                    .putString(KEY_COUNTRY, value.country)
                    .apply()
            }
    }
}