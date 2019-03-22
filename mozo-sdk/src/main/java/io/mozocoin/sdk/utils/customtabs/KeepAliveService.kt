package io.mozocoin.sdk.utils.customtabs

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class KeepAliveService : Service() {
    override fun onBind(intent: Intent?): IBinder? = binder

    companion object {
        private val binder: Binder = Binder()
    }
}