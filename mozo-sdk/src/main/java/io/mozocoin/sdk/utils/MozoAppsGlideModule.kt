package io.mozocoin.sdk.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.annotation.CallSuper
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

@Suppress("unused")
open class MozoAppsGlideModule : AppGlideModule() {
    @CallSuper
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val hasLowRam = hasLowRam(context)
        builder.setDefaultRequestOptions(RequestOptions().format(getBitmapQuality(hasLowRam)))
    }

    open fun requestLowBitmap() = false

    private fun getBitmapQuality(hasLowRam: Boolean): DecodeFormat {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || hasLowRam || requestLowBitmap()) {
            // return worse bitmap quality for low end devices
            DecodeFormat.PREFER_RGB_565
        } else {
            DecodeFormat.PREFER_ARGB_8888
        }
    }

    private fun hasLowRam(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.lowMemory
    }
}