package io.mozocoin.sdk.common

import java.util.*

class Constant {
    companion object {
        const val PAGING_START_INDEX = 0
        const val PAGING_SIZE = 100
        const val LIST_VISIBLE_THRESHOLD = 10

        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_FAILED = "FAILED"

        const val NOTIFY_EVENT_AIRDROPPED = "airdropped"
        const val NOTIFY_EVENT_AIRDROP_INVITE = "airdrop_invite"
        const val NOTIFY_EVENT_AIRDROP_FOUNDER = "airdrop_founder"
        const val NOTIFY_EVENT_AIRDROP_SIGN_UP = "airdrop_signup"
        const val NOTIFY_EVENT_AIRDROP_TOP_RETAILER = "airdrop_top_retailer"
        const val NOTIFY_EVENT_BALANCE_CHANGED = "balance_changed"
        const val NOTIFY_EVENT_CUSTOMER_CAME = "customer_came"
        const val NOTIFY_EVENT_ADDRESS_BOOK_CHANGED = "address_book_changed"
        const val NOTIFY_EVENT_STORE_BOOK_ADDED = "store_book_added"
        const val NOTIFY_EVENT_CONVERT = "convert_onchain_to_offchain"
        const val NOTIFY_EVENT_PROFILE_CHANGED = "profile_changed"
        const val NOTIFY_EVENT_PROMO_USED = "promotion_used"
        const val NOTIFY_EVENT_PROMO_PURCHASED = "promotion_purchased"
        const val NOTIFY_EVENT_GROUP_BROADCAST = "group_broadcast"
        const val NOTIFY_EVENT_WARNING_COVID = "covid_event"
        const val NOTIFY_EVENT_LUCKY_DRAW_AWARD = "lucky_draw_award"
        const val NOTIFY_EVENT_INVITATION_SETUP_EVENT = "invitation_setup_event"

        internal const val DEFAULT_DECIMAL = 2
        internal const val DEFAULT_CURRENCY = "USD"
        internal const val DEFAULT_CURRENCY_SYMBOL = "$"
        internal const val DEFAULT_CURRENCY_RATE = 0.000403004625212

        internal const val CURRENCY_SYMBOL_KRW = "₩"
        internal const val CURRENCY_SYMBOL_VND = "₫"

        internal const val DOMAIN_API_DEV = "dev.gateway.mozocoin.io"
        internal const val DOMAIN_API_STAGING = "staging.gateway.mozocoin.io"
        internal const val DOMAIN_API_PRODUCTION = "gateway.mozocoin.io"

        internal const val DOMAIN_AUTH_DEV = "dev.keycloak.mozocoin.io"
        internal const val DOMAIN_AUTH_STAGING = "staging.login.mozocoin.io"
        internal const val DOMAIN_AUTH_PRODUCTION = "login.mozocoin.io"

        internal const val DOMAIN_SOCKET_DEV = "dev.noti.mozocoin.io"
        internal const val DOMAIN_SOCKET_STAGING = "staging.noti.mozocoin.io"
        internal const val DOMAIN_SOCKET_PRODUCTION = "noti.mozocoin.io"

        internal const val DOMAIN_ETHER_SCAN_DEV = "ropsten.etherscan.io"
        internal const val DOMAIN_ETHER_SCAN_STAGING = "ropsten.etherscan.io"
        internal const val DOMAIN_ETHER_SCAN_PRODUCTION = "etherscan.io"

        internal const val DOMAIN_LANDING_PAGE_DEV = "dev.mozocoin.io"
        internal const val DOMAIN_LANDING_PAGE_STAGING = "staging.mozocoin.io"
        internal const val DOMAIN_LANDING_PAGE_PRODUCTION = "mozocoin.io"

        internal const val DOMAIN_IMAGE_DEV = "dev.image.mozocoin.io"
        internal const val DOMAIN_IMAGE_STAGING = "staging.image.mozocoin.io"
        internal const val DOMAIN_IMAGE_PRODUCTION = "image.mozocoin.io"

        internal const val SOCKET_CHANNEL_SHOPPER = "shopper"
        internal const val SOCKET_CHANNEL_RETAILER = "retailer"
        internal const val SOCKET_RETRY_START_TIME = 5000L

        private val ALPHABETS = charArrayOf('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '#')
        private val ALPHABETS_KOREA = charArrayOf('ㄱ', 'ㄴ', 'ㄷ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅅ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ', *ALPHABETS)

        fun getAlphabets() = if (Locale.getDefault().language == Locale.KOREA.language) ALPHABETS_KOREA else ALPHABETS
    }
}