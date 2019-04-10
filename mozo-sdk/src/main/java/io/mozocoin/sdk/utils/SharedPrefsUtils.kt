package io.mozocoin.sdk.utils

import android.preference.PreferenceManager
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.common.Constant
import java.math.BigDecimal

class SharedPrefsUtils private constructor() {
    companion object {
        private const val KEY_NEED_SYNC_WALLET = "SYNC_WALLET_INFO"

        private const val KEY_CURRENCY_RATE = "KEY_CURRENCY_RATE"

        @JvmStatic
        private fun manager() = PreferenceManager.getDefaultSharedPreferences(MozoSDK.getInstance().context.applicationContext)

        @JvmStatic
        fun isNeedSyncWallet() = manager().getBoolean(KEY_NEED_SYNC_WALLET, false)

        @JvmStatic
        fun setNeedSyncWallet(value: Boolean) {
            manager().edit().putBoolean(KEY_NEED_SYNC_WALLET, value).apply()
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
    }
}