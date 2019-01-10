package com.biglabs.mozo.sdk.common

class Constant {
    companion object {
        const val PAGING_START_INDEX = 0
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

        internal const val DOMAIN_API_DEV = "dev.gateway.mozocoin.io"
        internal const val DOMAIN_API_STAGING = "staging.gateway.mozocoin.io"
        internal const val DOMAIN_API_PRODUCTION = "staging.gateway.mozocoin.io"

        internal const val DOMAIN_AUTH_DEV = "dev.keycloak.mozocoin.io"
        internal const val DOMAIN_AUTH_STAGING = "staging.keycloak.mozocoin.io"
        internal const val DOMAIN_AUTH_PRODUCTION = "staging.keycloak.mozocoin.io"

        internal const val DOMAIN_SOCKET_DEV = "18.136.38.11:8089"
        internal const val DOMAIN_SOCKET_STAGING = "52.76.238.125:8089"
        internal const val DOMAIN_SOCKET_PRODUCTION = "52.76.238.125:8089"

        internal const val SOCKET_CHANNEL_SHOPPER = "shopper"
        internal const val SOCKET_CHANNEL_RETAILER = "retailer"
    }
}