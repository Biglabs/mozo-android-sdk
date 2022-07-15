package io.mozocoin.sdk

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import io.mozocoin.sdk.utils.RuntimeLocaleChanger
import okhttp3.OkHttpClient

open class MozoApp : Application(), ViewModelStoreOwner, ImageLoaderFactory {

    private val appVMS: ViewModelStore by lazy { ViewModelStore() }

    companion object {
        @Volatile
        lateinit var instance: MozoApp
    }

    @CallSuper
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(RuntimeLocaleChanger.wrapContext(base))
    }

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    @CallSuper
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        RuntimeLocaleChanger.overrideLocale(this)
    }

    override fun getViewModelStore(): ViewModelStore = appVMS

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .allowHardware(true)
            .allowRgb565(true)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(applicationContext).maxSizePercent(0.3).build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(applicationContext.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.2)
                    .build()
            }
            .logger(
                if (MozoSDK.isEnableDebugLogging) DebugLogger(Log.ERROR) else null
            )
            .okHttpClient {
                OkHttpClient.Builder().build()
            }
            .build()
    }
}