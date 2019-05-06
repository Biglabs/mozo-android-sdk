package io.mozocoin.sdk.common

internal object MessageEvent {
    class Pin(val pin: String, val requestCode: Int)
    class Auth(val exception: Exception? = null)
    class CloseActivities
    class ConvertOnChain
    class UserCancel
    class UserCancelErrorDialog
}