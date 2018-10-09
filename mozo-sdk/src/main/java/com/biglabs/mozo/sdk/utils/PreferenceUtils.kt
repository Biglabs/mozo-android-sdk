package com.biglabs.mozo.sdk.utils

import android.content.Context
import android.content.SharedPreferences
import com.biglabs.mozo.sdk.BuildConfig

internal class PreferenceUtils private constructor(private val sharedPreferences: SharedPreferences) {

    fun getFlag(flag: String) = sharedPreferences.getBoolean(flag, false)

    fun setFlag(flag: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(flag, value).apply()
    }

    fun getDecimal() = sharedPreferences.getInt(KEY_DECIMAL, 0)

    @Synchronized
    fun setDecimal(decimal: Int) {
        if (decimal > 0)
            sharedPreferences.edit().putInt(KEY_DECIMAL, decimal).apply()
    }

    companion object {
        const val FLAG_SYNC_WALLET_INFO = "SYNC_WALLET_INFO"

        const val KEY_DECIMAL = "KEY_DECIMAL"

        @Volatile
        private var instance: PreferenceUtils? = null

        internal fun getInstance(context: Context): PreferenceUtils =
                instance ?: synchronized(this) {
                    instance = PreferenceUtils(
                            context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                    )
                    return instance!!
                }

    }
}