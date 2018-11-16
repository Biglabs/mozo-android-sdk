package com.biglabs.mozo.sdk.common

class Constant {
    companion object {
        const val BASE_API_URL = "https://dev.gateway.mozocoin.io/solomon/api/"
        const val BASE_SOCKET_URL = "ws://18.136.38.11:8089/websocket/user/"

        const val PAGING_START_INDEX = 1
        const val PAGING_SIZE = 15
        const val LIST_VISIBLE_THRESHOLD = 5

        const val HISTORY_TIME_FORMAT = "HH:mm MMM dd, yyyy"

        const val SYMBOL_MOZO = "MOZOX"

        const val CURRENCY_KOREA = "KRW"
        const val CURRENCY_USA = "USD"

        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_FAILED = "FAILED"

        const val NOTIFY_EVENT_AIRDROPPED = "airdropped"
        const val NOTIFY_EVENT_BALANCE_CHANGED = "balance_changed"
        const val NOTIFY_EVENT_CUSTOMER_CAME = "customer_came"
    }
}