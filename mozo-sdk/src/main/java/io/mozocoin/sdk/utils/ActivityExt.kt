package io.mozocoin.sdk.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager

fun Activity.setMatchParent() {
    val attrs = window.attributes
    attrs.width = ViewGroup.LayoutParams.MATCH_PARENT
    attrs.height = ViewGroup.LayoutParams.MATCH_PARENT
    window.attributes = attrs
}

inline fun <reified T : Any> Activity.launchActivity(
        requestCode: Int = -1,
        options: Bundle? = null,
        noinline init: Intent.() -> Unit = {}) {
    val intent = newIntent<T>(this)
    intent.init()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        startActivityForResult(intent, requestCode, options)
    } else {
        startActivityForResult(intent, requestCode)
    }
}

inline fun <reified T : Any> Context.launchActivity(
        options: Bundle? = null,
        noinline init: Intent.() -> Unit = {}) {
    val intent = newIntent<T>(this)
    intent.init()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        startActivity(intent, options)
    } else {
        startActivity(intent)
    }
}

inline fun <reified T : Any> newIntent(context: Context): Intent =
        Intent(context, T::class.java)

fun Activity.adjustFontScale(scale: Float = 1.30f) {
    val configuration = resources.configuration
    if (configuration.fontScale > scale) {

        configuration.fontScale = scale
        val metrics = resources.displayMetrics
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(metrics)
        metrics.scaledDensity = configuration.fontScale * metrics.density
        @Suppress("DEPRECATION")
        baseContext.resources.updateConfiguration(configuration, metrics)
    }
}