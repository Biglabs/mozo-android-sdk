package io.mozocoin.sdk.utils

import androidx.preference.PreferenceManager
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.common.Constant
import java.math.BigDecimal

class SharedPrefsUtils private constructor() {
    companion object {
        private const val KEY_CURRENCY_RATE = "KEY_CURRENCY_RATE"
        private const val KEY_SHOW_AUTO_PIN_NOTICE = "KEY_SHOW_AUTO_PIN_NOTICE"

        private const val KEY_CONVERT_ON_IN_OFF_ADDRESS_LAST_TX = "KEY_CONVERT_ON_IN_OFF_ADDRESS_LAST_TX"
        private const val KEY_CONVERT_ON_IN_OFF_ADDRESS_AMOUNT = "KEY_CONVERT_ON_IN_OFF_ADDRESS_AMOUNT"

        @JvmStatic
        private fun manager() = PreferenceManager.getDefaultSharedPreferences(MozoSDK.getInstance().context.applicationContext)

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
        fun getShowAutoPinNotice(): Boolean {
            return manager().getBoolean(KEY_SHOW_AUTO_PIN_NOTICE, true)
        }

        @JvmStatic
        fun setShowAutoPinNotice(isShow: Boolean) {
            manager().edit().putBoolean(KEY_SHOW_AUTO_PIN_NOTICE, isShow).apply()
        }
    }
}