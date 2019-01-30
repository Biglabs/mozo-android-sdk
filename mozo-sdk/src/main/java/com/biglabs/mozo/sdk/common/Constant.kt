package com.biglabs.mozo.sdk.common

import java.util.*

class Constant {
    companion object {

        internal const val DEFAULT_DECIMAL = 2
        internal const val DEFAULT_CURRENCY = "USD"
        internal const val DEFAULT_CURRENCY_SYMBOL = "$"
        internal const val DEFAULT_CURRENCY_RATE = 0.0000691440330774

        const val PAGING_START_INDEX = 0
        const val PAGING_SIZE = 15
        const val LIST_VISIBLE_THRESHOLD = 5

        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_FAILED = "FAILED"

        const val NOTIFY_EVENT_AIRDROPPED = "airdropped"
        const val NOTIFY_EVENT_BALANCE_CHANGED = "balance_changed"
        const val NOTIFY_EVENT_CUSTOMER_CAME = "customer_came"
        const val NOTIFY_EVENT_STORE_BOOK_ADDED = "store_book_added"

        internal const val DOMAIN_API_DEV = "dev.gateway.mozocoin.io"
        internal const val DOMAIN_API_STAGING = "staging.gateway.mozocoin.io"
        internal const val DOMAIN_API_PRODUCTION = "staging.gateway.mozocoin.io"

        internal const val DOMAIN_AUTH_DEV = "dev.keycloak.mozocoin.io"
        internal const val DOMAIN_AUTH_STAGING = "staging.keycloak.mozocoin.io"
        internal const val DOMAIN_AUTH_PRODUCTION = "staging.keycloak.mozocoin.io"

        internal const val DOMAIN_SOCKET_DEV = "dev.noti.mozocoin.io"
        internal const val DOMAIN_SOCKET_STAGING = "staging.noti.mozocoin.io"
        internal const val DOMAIN_SOCKET_PRODUCTION = "noti.mozocoin.io"

        internal const val SOCKET_CHANNEL_SHOPPER = "shopper"
        internal const val SOCKET_CHANNEL_RETAILER = "retailer"
        internal const val SOCKET_RETRY_START_TIME = 5000L

        private val ALPHABETS = charArrayOf('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '#')
        private val ALPHABETS_KOREA = charArrayOf('ㄱ', 'ㄴ', 'ㄷ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅅ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ', *ALPHABETS)

        fun getAlphabets() = if (Locale.getDefault().language == Locale.KOREA.language) Constant.ALPHABETS_KOREA else Constant.ALPHABETS
    }
}