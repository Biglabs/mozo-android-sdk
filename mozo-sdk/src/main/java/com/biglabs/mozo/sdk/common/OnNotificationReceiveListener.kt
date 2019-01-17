package com.biglabs.mozo.sdk.common

import com.biglabs.mozo.sdk.common.model.Notification

interface OnNotificationReceiveListener {
    fun onReceived(notification: Notification)
}