package io.mozocoin.sdk.ui

import android.content.Context
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.splitcompat.SplitCompat
import io.mozocoin.sdk.utils.RuntimeLocaleChanger

abstract class LocalizationBaseActivity : AppCompatActivity() {
    @CallSuper
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(RuntimeLocaleChanger.wrapContext(base))
        SplitCompat.installActivity(this)
    }
}