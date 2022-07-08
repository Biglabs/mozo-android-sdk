package io.mozocoin.sdk.utils

import android.content.Context
import android.content.res.Configuration
import java.util.*

object RuntimeLocaleChanger {
    fun wrapContext(context: Context): Context {
        SharedPrefsUtils.manager(context)
        val savedLocale = SharedPrefsUtils.language

        // as part of creating a new context that contains the new locale we also need to override the default locale.
        Locale.setDefault(savedLocale)

        // create new configuration with the saved locale
        val newConfig = Configuration()
        newConfig.setLocale(savedLocale)
        return context.createConfigurationContext(newConfig)
    }

    fun overrideLocale(context: Context) {
        SharedPrefsUtils.manager(context)
        val savedLocale = SharedPrefsUtils.language

        // as part of creating a new context that contains the new locale we also need to override the default locale.
        Locale.setDefault(savedLocale)

        // create new configuration with the saved locale
        val newConfig = Configuration()
        newConfig.setLocale(savedLocale)

        // override the locale on the given context (Activity, Fragment, etc...)
        context.resources.updateConfiguration(newConfig, context.resources.displayMetrics)

        // override the locale on the application context
        if (context != context.applicationContext) {
            context.applicationContext.resources.run {
                updateConfiguration(
                    newConfig,
                    displayMetrics
                )
            }
        }
    }
}