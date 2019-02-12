package io.mozocoin.sdk.utils

import android.preference.PreferenceManager
import io.mozocoin.sdk.MozoSDK
import io.mozocoin.sdk.common.Constant

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
        fun getDefaultCurrencyRate(): Double {
            val rate = manager().getString(KEY_CURRENCY_RATE, "")
            return if (rate.isNullOrEmpty()) Constant.DEFAULT_CURRENCY_RATE
            else rate.toDouble()
        }

        @JvmStatic
        fun setDefaultCurrencyRate(rate: Double) {
            val rateStr = if (rate <= 0) Constant.DEFAULT_CURRENCY_RATE.toString()
            else rate.toString()
            manager().edit().putString(KEY_CURRENCY_RATE, rateStr).apply()
        }
    }
}