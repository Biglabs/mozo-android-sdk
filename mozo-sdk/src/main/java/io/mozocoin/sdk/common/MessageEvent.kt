package io.mozocoin.sdk.common

internal object MessageEvent {
    class Pin(val pin: String, val requestCode: Int)
    class Auth(val exception: Exception? = null)
    class CreateWalletAutomatic
    class CloseActivities
    class ConvertOnChain
    class StopMaintenanceMode
    class UserCancel
    class UserCancelErrorDialog
}