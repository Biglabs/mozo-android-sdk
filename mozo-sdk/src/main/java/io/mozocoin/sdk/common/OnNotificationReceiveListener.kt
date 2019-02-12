package io.mozocoin.sdk.common

import io.mozocoin.sdk.common.model.Notification

interface OnNotificationReceiveListener {
    fun onReceived(notification: Notification)
}