package com.biglabs.mozo.sdk.common

object MessageEvent {
    class Pin(val pin: String, val requestCode: Int)
    class Auth(val isSignedIn: Boolean, val exception: Exception? = null)
}